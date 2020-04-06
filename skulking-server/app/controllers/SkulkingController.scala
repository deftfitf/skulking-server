package controllers

import actor.{PlayerActor, PlayersActor, RoomsActor}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem => TypedActorSystem}
import akka.actor.{Actor, ActorRefFactory, ActorSystem, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy, Terminated, ActorRef => ClassicActorRef}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import controllers.SkulkingController.{PlayerRepositoryError, SessionIdNotFound}
import jp.skulking.domain.SessionId
import jp.skulking.header.SessionHeader
import jp.skulking.protocol._
import play.api.Environment
import play.api.libs.json.JsValue
import play.api.mvc._
import repository.PlayerRepository

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SkulkingController(
                          cc: ControllerComponents,
                          roomsActorRef: ActorRef[RoomsActor.Protocol],
                          playersActor: ActorRef[PlayersActor.Protocol],
                          playerRepository: PlayerRepository,
                          clientFormats: ClientProtocolJSONFormats,
                          serverFormats: ServerProtocolJSONFormats)
                        (implicit env: Environment, actorSystem: ActorSystem, m: Materializer)
  extends AbstractController(cc) {

  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val typedSystem: TypedActorSystem[_] = actorSystem.toTyped
  implicit val timeout: Timeout = 3.seconds

  def index = Action {
    Ok(views.html.index(env))
  }

  def login = Action(parse.json) { request =>
    LoginRequest.defaultLoginRequestFormat.reads(request.body).asOpt match {
      case Some(body) =>
        val response = LoginResponse(Some(body.playerId), None)
        Ok(LoginResponse.defaultLoginResponseFormat.writes(response))
      case None => BadRequest
    }
  }

  def registry = Action(parse.json) { request =>
    RegistryRequest.defaultRegistryRequestFormat.reads(request.body).asOpt match {
      case Some(body) =>
        val response = RegistryResponse(Some(body.playerId), None)
        Ok(RegistryResponse.defaultRegistryResponseFormat.writes(response))
      case None => BadRequest
    }
  }

  def rooms = Action.async { request =>
    (for {
      sessionId <- EitherT.fromOption[Future](request.headers.get(SessionHeader.name).map(SessionId), SessionIdNotFound)
      _ <- EitherT(playerRepository.findBy(sessionId).map(_.left.map(PlayerRepositoryError.apply)))
      rooms <- EitherT(
        roomsActorRef.ask[RoomsActor.RoomListResponse](rsp => RoomsActor.Protocol.RoomList(rsp, 0))
          .map(_.rooms.asRight[SkulkingController.Error]))
    } yield {
      val response = RoomsResponse(rooms.map(_.value))
      Ok(RoomsResponse.defaultRoomsResponseFormat.writes(response))
    }).value.map {
      case Right(r) => r
      case Left(e) => errorToResult(e)
    }
  }

  def connect: WebSocket =
    WebSocket.acceptOrResult[JsValue, JsValue] { _ =>
      (for {
        skulkingFlow <- EitherT(actorFlow[PlayerActor.ClientProtocolWrapper, ServerProtocol](out => {
          val embedBehavior = PlayerActor.certificating(out.toTyped[ServerProtocol], playerRepository, roomsActorRef)
          playersActor.ask[PlayersActor.Response.Created](
            rsp => PlayersActor.Protocol.CreatePlayer(rsp, embedBehavior))
            .map(_.playerActorRef.narrow[PlayerActor.ClientProtocolWrapper])
        }).map(_.asRight[SkulkingController.Error]))
        inAdapterFlow = Flow.fromFunction(
          PlayerActor.ClientProtocolWrapper.apply _ compose clientFormats.deserialize)
        outAdapterFlow = Flow.fromFunction(serverFormats.serialize)
      } yield {
        inAdapterFlow.via(skulkingFlow).via(outAdapterFlow)
      }).value.map(_.left.map(errorToResult))
    }

  private def errorToResult(error: SkulkingController.Error): Result =
    error match {
      case PlayerRepositoryError(_) => Results.InternalServerError
      case SessionIdNotFound => Results.BadRequest
    }

  private def actorFlow[In, Out](
                                  behaviorFactory: ClassicActorRef => Future[ActorRef[In]],
                                  bufferSize: Int = 16,
                                  overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew)
                                (implicit factory: ActorRefFactory): Future[Flow[In, Out, _]] = {
    val (outActor, publisher) = Source
      .actorRef[Out](bufferSize, overflowStrategy)
      .toMat(Sink.asPublisher(false))(Keep.both)
      .run()

    for {
      actorRef <- behaviorFactory(outActor)
    } yield {
      Flow.fromSinkAndSource[In, Out](
        Sink.actorRef(
          factory.actorOf(Props(new Actor {
            val flowActor: ClassicActorRef = context.watch(actorRef.toClassic)

            def receive: Receive = {
              case akka.actor.Status.Success(_) | akka.actor.Status.Failure(_) => flowActor ! PoisonPill
              case Terminated(_) => context.stop(self)
              case other => flowActor ! other
            }

            override def supervisorStrategy = OneForOneStrategy() {
              case _ => SupervisorStrategy.Stop
            }
          })),
          akka.actor.Status.Success(())
        ),
        Source.fromPublisher(publisher)
      )
    }
  }

}

object SkulkingController {

  sealed trait Error

  case class PlayerRepositoryError(error: PlayerRepository.Error) extends Error

  case object SessionIdNotFound extends Error

}