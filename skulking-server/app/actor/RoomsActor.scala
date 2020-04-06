package actor

import java.util.UUID

import actor.RoomsActor.Protocol._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import jp.skulking.domain.{CardDecks, DeckId, PlayerId, RoomId}

object RoomsActor {

  sealed trait Protocol

  object Protocol {

    case class Lookup(sender: ActorRef[LookupResponse], roomId: RoomId) extends Protocol

    case class CreateRoom(
                           sender: ActorRef[CreatedResponse],
                           masterId: PlayerId,
                           nOfRound: Int,
                           deckId: DeckId,
                           roomMaxSize: Int,
                           masterActorRef: ActorRef[PlayerActor.Protocol]) extends Protocol

    case class DeleteRoom(roomId: RoomId) extends Protocol

    case class RoomTerminated(roomId: RoomId) extends Protocol

    case class RoomList(sender: ActorRef[RoomListResponse], page: Int) extends Protocol

  }

  case class LookupResponse(roomId: RoomId, skulkingActorRefOpt: Option[ActorRef[SkulkingActor.Protocol]])

  case class CreatedResponse(roomId: RoomId, skulkingActorRef: ActorRef[SkulkingActor.Protocol])

  case class RoomListResponse(rooms: List[RoomId])

  def apply(rooms: Map[RoomId, ActorRef[SkulkingActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup { context =>

      Behaviors.receiveMessage {
        case Lookup(sender, roomId) =>
          sender ! LookupResponse(roomId, rooms.get(roomId))
          Behaviors.same
        case CreateRoom(sender, masterId, nOfRound, deckId, roomMaxSize, masterActorRef) =>
          val deck = CardDecks.decks(deckId)
          val roomId = RoomId(UUID.randomUUID().toString)
          val newRoom = context.spawn(
            SkulkingActor.initializing(roomId, masterId, nOfRound, deck, roomMaxSize, masterId :: Nil, masterActorRef :: Nil),
            roomId.value.toString)
          context.watchWith(newRoom, RoomTerminated(roomId))

          sender ! CreatedResponse(roomId, newRoom)
          apply(rooms.updated(roomId, newRoom))
        case DeleteRoom(roomId) =>
          apply(rooms - roomId)
        case RoomTerminated(roomId) =>
          apply(rooms - roomId)
        case RoomList(sender, _) =>
          sender ! RoomListResponse(rooms.keys.toList)
          Behaviors.same

      }
    }

}
