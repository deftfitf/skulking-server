package actor

import actor.PlayersActor.Protocol.CreatePlayer
import actor.PlayersActor.Response.Created
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object PlayersActor {

  sealed trait Protocol

  object Protocol {

    case class CreatePlayer(sender: ActorRef[Response.Created], behavior: Behavior[PlayerActor.Protocol]) extends Protocol

  }

  sealed trait Response

  object Response {

    case class Created(playerActorRef: ActorRef[PlayerActor.Protocol])

  }

  def apply(): Behavior[PlayersActor.Protocol] =
    Behaviors.setup { context =>

      Behaviors.receiveMessage {
        case CreatePlayer(sender, behavior) =>
          val playerRef = context.spawnAnonymous(behavior)
          sender ! Created(playerRef)
          Behaviors.same
      }
    }

}
