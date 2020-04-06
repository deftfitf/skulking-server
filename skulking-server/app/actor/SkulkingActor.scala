package actor

import actor.SkulkingActor.Protocol._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import jp.skulking.domain.Card.{PiratesEvent, RascalOfRoatan, Tigress}
import jp.skulking.domain.Skulking._
import jp.skulking.domain._

object SkulkingActor {

  private val ROOM_MIN_SIZE = 2

  sealed trait Protocol

  object Protocol {

    case class Join(playerId: PlayerId, playerActorRef: ActorRef[PlayerActor.Protocol]) extends Protocol

    case class Leave(playerId: PlayerId) extends Protocol

    case class DeActive(playerId: PlayerId) extends Protocol

    case class Start(playerId: PlayerId) extends Protocol

    case class BidDeclareChange(playerId: PlayerId, declareBid: Int) extends Protocol

    case class FuturePredicateFinish(playerId: PlayerId) extends Protocol

    case class PlayTrick(playerId: PlayerId, cardId: CardId) extends Protocol

    case class PlayTigress(playerId: PlayerId, cardId: CardId, isPirates: Boolean) extends Protocol

    case class PlayRascalOfRoatan(playerId: PlayerId, cardId: CardId, betScore: Int) extends Protocol

    case class ChangeDealer(playerId: PlayerId, newDealerId: PlayerId) extends Protocol

    case class ChangeHand(playerId: PlayerId, returnCards: List[CardId]) extends Protocol

    case object Replay extends Protocol

    case object Finish extends Protocol

  }

  sealed trait SkulkingEvent

  object SkulkingEvent {

    case class NewPlayerJoined(playerId: PlayerId) extends SkulkingEvent

    case class MasterLeave(leaveMater: PlayerId, nextMaster: PlayerId) extends SkulkingEvent

    case class PlayerLeave(playerId: PlayerId) extends SkulkingEvent

    case class GameStarted() extends SkulkingEvent

    case class NewRoundStarted(round: Int, hands: Map[PlayerId, Set[CardId]], restOfDeck: Int) extends SkulkingEvent

    case class BidDeclared(playerId: PlayerId) extends SkulkingEvent

    case class TrickPhaseStarted(bidDeclares: List[BidDeclare]) extends SkulkingEvent

    case class Played(playerId: PlayerId, card: CardId) extends SkulkingEvent

    case class TigressPlayed(playerId: PlayerId, cardId: CardId, isPirates: Boolean) extends SkulkingEvent

    case class RascalOfRoatanPlayed(playerId: PlayerId, cardId: CardId, betScore: Int) extends SkulkingEvent

    case class TrickFinished(trickWinner: PlayerId) extends SkulkingEvent

    case class PiratesEventWrapper(piratesEvent: PiratesEvent) extends SkulkingEvent

    case class FuturePredicateFinished(playerId: PlayerId) extends SkulkingEvent

    case class BidDeclareChanged(playerId: PlayerId, changeBid: Int) extends SkulkingEvent

    case class NextPlay(nextDealerId: PlayerId) extends SkulkingEvent

    case class RoundFinished(round: Int, roundScore: RoundScore) extends SkulkingEvent

    case class GameFinished(gameWinner: PlayerId, gameScore: Map[PlayerId, Int]) extends SkulkingEvent

    case class NextTrickChanged(from: PlayerId, to: PlayerId) extends SkulkingEvent

    case class HandChanged(changed: PlayerId, nOfChanged: Int) extends SkulkingEvent

    case class ReplayGame() extends SkulkingEvent

    case class ShutdownGame() extends SkulkingEvent

  }

  def broadcastEvent(playerActors: List[ActorRef[PlayerActor.Protocol]], skulkingEvents: SkulkingEvent*): Unit =
    for {
      playerActor <- playerActors
      skulkingEvent <- skulkingEvents
    } playerActor ! PlayerActor.SkulkingEventWrap(skulkingEvent)

  def initializing(roomId: RoomId, masterId: PlayerId, nOfRound: Int, deck: Deck, roomMaxSize: Int, players: List[PlayerId], playerActors: List[ActorRef[PlayerActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup[Protocol] { context =>
      context.log.info(s"room master $masterId is new room created.")

      Behaviors.receiveMessagePartial[Protocol] {
        case Join(playerId, playerActorRef) =>
          if (players.size >= roomMaxSize) {
            playerActorRef ! PlayerActor.JoinFailed("The maximum number of participants has been exceeded")
            Behaviors.same
          } else if (!players.contains(playerId)) {
            val newPlayers = players :+ playerId
            playerActorRef ! PlayerActor.JoinSucceeded(roomId, masterId, nOfRound, deck.id, roomMaxSize, newPlayers)
            broadcastEvent(playerActors, SkulkingEvent.NewPlayerJoined(playerId))
            initializing(roomId, masterId, nOfRound, deck, roomMaxSize, newPlayers, playerActorRef :: playerActors)
          } else {
            val idx = (players.length - players.indexOf(playerId) - 1)
            val playersActorRef = playerActors.updated(idx, playerActorRef)

            playerActorRef ! PlayerActor.JoinSucceeded(roomId, masterId, nOfRound, deck.id, roomMaxSize, players)
            initializing(roomId, masterId, nOfRound, deck, roomMaxSize, players, playersActorRef)
          }
        case Leave(playerId) =>
          if (masterId == playerId) {
            if (players.nonEmpty) {
              broadcastEvent(playerActors, SkulkingEvent.MasterLeave(playerId, players.head))
              initializing(roomId, players.head, nOfRound, deck, roomMaxSize, players.filterNot(_ == playerId), playerActors)
            } else {
              Behaviors.stopped(() => {})
            }
          } else if (players.contains(playerId)) {
            broadcastEvent(playerActors, SkulkingEvent.PlayerLeave(playerId))
            initializing(roomId, masterId, nOfRound, deck, roomMaxSize, players.filterNot(_ == playerId), playerActors)
          } else {
            Behaviors.unhandled
          }
        case Start(pId) if pId == masterId =>
          if (players.size < ROOM_MIN_SIZE) {
            // TODO: message something
            Behaviors.same
          } else {
            val newSkulking = Skulking.newGame(masterId, players, nOfRound, deck)

            broadcastEvent(playerActors,
              SkulkingEvent.GameStarted(),
              SkulkingEvent.NewRoundStarted(
                newSkulking.round.currentRound,
                newSkulking.round.currentPlayerHands,
                newSkulking.round.deck.size))

            bidding(roomId, newSkulking, masterId, players, Nil, playerActors)
          }
      }
    }

  def bidding(
               roomId: RoomId, biddingPhase: BiddingPhase,
               masterId: PlayerId, players: List[PlayerId],
               bidDeclares: List[BidDeclare],
               playerActors: List[ActorRef[PlayerActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup[Protocol] { _ =>

      Behaviors.receiveMessagePartial[Protocol] {
        case Join(playerId, playerActorRef) =>
          if (players.contains(playerId)) {
            val idx = (players.length - players.indexOf(playerId) - 1)
            val playersActorRef = playerActors.updated(idx, playerActorRef)

            playerActorRef ! PlayerActor.GameSnapshot(roomId, masterId, biddingPhase)
            if (!bidDeclares.exists(_.playerId == playerId)) {
              playerActorRef ! PlayerActor.SkulkingEventWrap(
                SkulkingEvent.NewRoundStarted(
                  biddingPhase.round.currentRound,
                  biddingPhase.round.currentPlayerHands,
                  biddingPhase.round.deck.size))
            }
            bidding(roomId, biddingPhase, masterId, players, bidDeclares, playersActorRef)
          } else {
            Behaviors.unhandled
          }

        case BidDeclareChange(playerId, declareBid) =>
          if (players.contains(playerId) &&
            !bidDeclares.map(_.playerId).contains(playerId)) {
            val newBidDeclares = BidDeclare(playerId, declareBid) :: bidDeclares
            if (newBidDeclares.length < players.length) {
              broadcastEvent(playerActors, SkulkingEvent.BidDeclared(playerId))
              bidding(roomId, biddingPhase, masterId, players, newBidDeclares, playerActors)
            } else {
              broadcastEvent(playerActors,
                SkulkingEvent.BidDeclared(playerId),
                SkulkingEvent.TrickPhaseStarted(newBidDeclares))
              tricking(roomId, biddingPhase.bid(newBidDeclares.map(d => (d.playerId, d)).toMap), masterId, players, playerActors)
            }
          } else {
            Behaviors.unhandled
          }
        case DeActive(playerId) =>
          //
          Behaviors.same
      }
    }

  def tricking(
                roomId: RoomId, trickPhase: TrickPhase,
                masterId: PlayerId, players: List[PlayerId],
                playerActors: List[ActorRef[PlayerActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup[Protocol] { _ =>

      def playTrick(playerId: PlayerId, card: Card, onSuccess: () => Unit): Behavior[Protocol] =
        trickPhase.play(playerId, card) match {
          case PlayResult.InvalidInput =>
            // TODO: if invalid, send message
            Behaviors.unhandled
          case PlayResult.PlaySuccess(trick) =>
            onSuccess()
            tricking(roomId, trick, masterId, players, playerActors)
          case PlayResult.TrickFinished(trick, lastWinnerId, piratesEvent) =>
            onSuccess()
            broadcastEvent(playerActors,
              SkulkingEvent.TrickFinished(lastWinnerId),
              SkulkingEvent.PiratesEventWrapper(piratesEvent))
            trick match {
              case trick: TrickPhase =>
                tricking(roomId, trick, masterId, players, playerActors)
              case dealerChanging: NextTrickLeadPlayerChanging =>
                nextTrickLeadPlayerChanging(roomId, dealerChanging, masterId, players, playerActors)
              case handChanging: HandChangeWaiting =>
                handChangeWaiting(roomId, handChanging, masterId, players, playerActors)
              case futurePredicating: FuturePredicateWaiting =>
                futurePredicateWaiting(roomId, futurePredicating, masterId, players, playerActors)
              case bidDeclareWaiting: BidDeclareChangeWaiting =>
                bidDeclareChangeWaiting(roomId, bidDeclareWaiting, masterId, players, playerActors)
            }
        }

      if (!trickPhase.round.isRoundFinished) {
        broadcastEvent(playerActors, SkulkingEvent.NextPlay(trickPhase.round.nextPlayerId))
        Behaviors.receiveMessagePartial[Protocol] {
          case Join(playerId, playerActorRef) =>
            if (players.contains(playerId)) {
              val idx = (players.length - players.indexOf(playerId) - 1)
              val playersActorRef = playerActors.updated(idx, playerActorRef)

              playerActorRef ! PlayerActor.GameSnapshot(roomId, masterId, trickPhase)
              if (trickPhase.round.nextPlayerId == playerId) {
                playerActorRef ! PlayerActor.SkulkingEventWrap(
                  SkulkingEvent.NextPlay(playerId))
              }
              tricking(roomId, trickPhase, masterId, players, playersActorRef)
            } else {
              Behaviors.unhandled
            }

          case PlayTrick(playerId, cardId) =>
            trickPhase.deck.of[Card](cardId) match {
              case Some(card) => playTrick(playerId, card,
                () => broadcastEvent(playerActors, SkulkingEvent.Played(playerId, cardId)))
              case None => Behaviors.unhandled
            }
          case PlayTigress(playerId, cardId, isPirates) =>
            trickPhase.deck.of[Tigress](cardId) match {
              case Some(tigress) => playTrick(playerId, tigress.pirates(isPirates),
                () => broadcastEvent(playerActors, SkulkingEvent.TigressPlayed(playerId, cardId, isPirates)))
              case None => Behaviors.unhandled
            }
          case PlayRascalOfRoatan(playerId, cardId, betScore) =>
            (for {
              rascalOfRoatan <- trickPhase.deck.of[RascalOfRoatan](cardId)
              roatan <- rascalOfRoatan.bet(betScore)
            } yield {
              playTrick(playerId, roatan,
                () => broadcastEvent(playerActors, SkulkingEvent.RascalOfRoatanPlayed(playerId, cardId, betScore)))
            }).getOrElse(Behaviors.unhandled)
          case DeActive(playerId) =>
            //
            Behaviors.same
        }
      } else if (!trickPhase.isLastRound) {
        val roundFinishedPhase = trickPhase.roundFinished()
        broadcastEvent(playerActors,
          SkulkingEvent.RoundFinished(trickPhase.round.currentRound, roundFinishedPhase.lastRoundScore),
          SkulkingEvent.NewRoundStarted(
            roundFinishedPhase.round.currentRound,
            roundFinishedPhase.round.currentPlayerHands,
            roundFinishedPhase.round.deck.size))
        bidding(roomId, roundFinishedPhase, masterId, players, Nil, playerActors)
      } else {
        val finishedPhase = trickPhase.finished()
        broadcastEvent(playerActors,
          SkulkingEvent.RoundFinished(trickPhase.round.currentRound, finishedPhase.lastRoundScore),
          SkulkingEvent.GameFinished(finishedPhase.gameWinnerId, finishedPhase.aggregate))
        finished(roomId, finishedPhase, masterId, players, playerActors)
      }
    }

  def nextTrickLeadPlayerChanging(
                                   roomId: RoomId, changing: NextTrickLeadPlayerChanging,
                                   masterId: PlayerId, players: List[PlayerId],
                                   playerActors: List[ActorRef[PlayerActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup[Protocol] { _ =>

      Behaviors.receiveMessagePartial[Protocol] {
        case Join(playerId, playerActorRef) =>
          if (players.contains(playerId)) {
            val idx = (players.length - players.indexOf(playerId) - 1)
            val playersActorRef = playerActors.updated(idx, playerActorRef)

            playerActorRef ! PlayerActor.GameSnapshot(roomId, masterId, changing)
            if (changing.changingPlayerId == playerId) {
              playerActorRef ! PlayerActor.SkulkingEventWrap(
                SkulkingEvent.PiratesEventWrapper(
                  PiratesEvent.NextTrickLeadPlayerChangeableNotice(playerId)))
            }
            nextTrickLeadPlayerChanging(roomId, changing, masterId, players, playersActorRef)
          } else {
            Behaviors.unhandled
          }

        case ChangeDealer(playerId, newDealerId) =>
          if (changing.changingPlayerId == playerId) {
            val trick = changing.changeLeadPlayer(newDealerId)
            broadcastEvent(playerActors, SkulkingEvent.NextTrickChanged(playerId, newDealerId))
            tricking(roomId, trick, masterId, players, playerActors)
          } else {
            Behaviors.unhandled
          }
        case DeActive(playerId) =>
          //
          Behaviors.same
      }
    }

  def handChangeWaiting(
                         roomId: RoomId, waiting: HandChangeWaiting,
                         masterId: PlayerId, players: List[PlayerId],
                         playerActors: List[ActorRef[PlayerActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup[Protocol] { _ =>

      Behaviors.receiveMessagePartial[Protocol] {
        case Join(playerId, playerActorRef) =>
          if (players.contains(playerId)) {
            val idx = (players.length - players.indexOf(playerId) - 1)
            val playersActorRef = playerActors.updated(idx, playerActorRef)

            playerActorRef ! PlayerActor.GameSnapshot(roomId, masterId, waiting)
            if (waiting.changingPlayerId == playerId) {
              playerActorRef ! PlayerActor.SkulkingEventWrap(
                SkulkingEvent.PiratesEventWrapper(
                  PiratesEvent.HandChangeAvailableNotice(playerId, waiting.drawCard)))
            }
            handChangeWaiting(roomId, waiting, masterId, players, playersActorRef)
          } else {
            Behaviors.unhandled
          }

        case ChangeHand(playerId, returnCards) =>
          if (waiting.changingPlayerId == playerId) {
            val trick = waiting.changeHand(returnCards)
            broadcastEvent(playerActors, SkulkingEvent.HandChanged(playerId, returnCards.length))
            tricking(roomId, trick, masterId, players, playerActors)
          } else {
            Behaviors.unhandled
          }
        case DeActive(playerId) =>
          //
          Behaviors.same
      }
    }

  def futurePredicateWaiting(
                              roomId: RoomId, waiting: FuturePredicateWaiting,
                              masterId: PlayerId, players: List[PlayerId],
                              playerActors: List[ActorRef[PlayerActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup[Protocol] { context =>

      Behaviors.receiveMessagePartial[Protocol] {
        case Join(playerId, playerActorRef) =>
          if (players.contains(playerId)) {
            val idx = (players.length - players.indexOf(playerId) - 1)
            val playersActorRef = playerActors.updated(idx, playerActorRef)

            playerActorRef ! PlayerActor.GameSnapshot(roomId, masterId, waiting)
            if (waiting.predicatingPlayerId == playerId) {
              playerActorRef ! PlayerActor.SkulkingEventWrap(
                SkulkingEvent.PiratesEventWrapper(
                  PiratesEvent.HandChangeAvailableNotice(playerId, waiting.trickPhase.round.deck.map(_.id))))
            }
            futurePredicateWaiting(roomId, waiting, masterId, players, playersActorRef)
          } else {
            Behaviors.unhandled
          }

        case FuturePredicateFinish(playerId) =>
          if (waiting.predicatingPlayerId == playerId) {
            broadcastEvent(playerActors, SkulkingEvent.FuturePredicateFinished(playerId))
            tricking(roomId, waiting.finish(), masterId, players, playerActors)
          } else {
            Behaviors.unhandled
          }
        case DeActive(playerId) =>
          //
          Behaviors.same
      }
    }

  def bidDeclareChangeWaiting(
                               roomId: RoomId, waiting: BidDeclareChangeWaiting,
                               masterId: PlayerId, players: List[PlayerId],
                               playerActors: List[ActorRef[PlayerActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup[Protocol] { context =>

      Behaviors.receiveMessagePartial[Protocol] {
        case Join(playerId, playerActorRef) =>
          if (players.contains(playerId)) {
            val idx = (players.length - players.indexOf(playerId) - 1)
            val playersActorRef = playerActors.updated(idx, playerActorRef)

            playerActorRef ! PlayerActor.GameSnapshot(roomId, masterId, waiting)
            if (waiting.changingPlayerId == playerId) {
              playerActorRef ! PlayerActor.SkulkingEventWrap(
                SkulkingEvent.PiratesEventWrapper(
                  PiratesEvent.DeclareBidChangeAvailable(playerId, waiting.)))
              // TODO: min, maxを与える
              // GamesnapshotでSkulking を渡して、Snapshotとしてシリアライズする。
            }
            bidDeclareChangeWaiting(roomId, waiting, masterId, players, playersActorRef)
          } else {
            Behaviors.unhandled
          }

        case BidDeclareChange(playerId, declareBid) =>
          if (waiting.changingPlayerId == playerId) {
            broadcastEvent(playerActors, SkulkingEvent.BidDeclareChanged(playerId, declareBid))
            tricking(roomId, waiting.change(playerId, declareBid), masterId, players, playerActors)
          } else {
            Behaviors.unhandled
          }
        case DeActive(playerId) =>
          //
          Behaviors.same
      }
    }

  def finished(
                roomId: RoomId, finishedPhase: Finished,
                masterId: PlayerId, players: List[PlayerId],
                playerActors: List[ActorRef[PlayerActor.Protocol]]): Behavior[Protocol] =
    Behaviors.setup[Protocol] { context =>

      Behaviors.receiveMessagePartial[Protocol] {
        case Replay =>
          val newSkulking = finishedPhase.replay()
          broadcastEvent(playerActors,
            SkulkingEvent.ReplayGame(),
            SkulkingEvent.NewRoundStarted(
              newSkulking.round.currentRound,
              newSkulking.round.currentPlayerHands,
              newSkulking.round.deck.size))
          bidding(roomId, newSkulking, masterId, players, Nil, playerActors)
        case Finish =>
          broadcastEvent(playerActors, SkulkingEvent.ShutdownGame())
          Behaviors.stopped
        case DeActive(playerId) =>
          //
          Behaviors.same
      }
    }

}
