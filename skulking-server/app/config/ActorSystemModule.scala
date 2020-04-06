package config

import actor.{PlayersActor, RoomsActor}
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object ActorSystemModule {

  implicit val system: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(supervisor(), "skulking")
  implicit val ec: ExecutionContext = system.executionContext
  implicit val timeout: Timeout = Timeout(3.seconds)

  lazy val roomsActor: ActorRef[RoomsActor.Protocol] =
    Await.result(spawn(RoomsActor.apply(Map.empty), "rooms"), 10.second)

  lazy val playersActor: ActorRef[PlayersActor.Protocol] =
    Await.result(spawn(PlayersActor.apply(), "rooms"), 10.second)

  def supervisor(): Behavior[SpawnProtocol.Command] =
    Behaviors.setup { context =>

      SpawnProtocol()
    }

  def spawn[T](behavior: Behavior[T], name: String): Future[ActorRef[T]] =
    system.ask[ActorRef[T]](SpawnProtocol.Spawn(behavior = behavior, name = name, props = Props.empty, _))

}
