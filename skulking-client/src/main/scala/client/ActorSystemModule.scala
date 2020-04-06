package client

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import client.scene.SkulkingBoardSceneActor

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object ActorSystemModule {

  implicit val system: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(SkulkingBoardSceneActor.supervisor(), "skulking")
  implicit val ec: ExecutionContext = system.executionContext
  implicit val timeout: Timeout = Timeout(3.seconds)

  def spawn[T](behavior: Behavior[T], name: String): Future[ActorRef[T]] =
    system.ask[ActorRef[T]](SpawnProtocol.Spawn(behavior = behavior, name = name, props = Props.empty, _))

}
