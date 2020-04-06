package actor

import actor.RoomsActor.{CreatedResponse, LookupResponse}
import actor.SkulkingActor.SkulkingEvent
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import jp.skulking.domain.Card.PiratesEvent
import jp.skulking.domain._
import jp.skulking.protocol.ClientProtocol.Disconnect
import jp.skulking.protocol.{ClientProtocol, ServerProtocol}
import repository.PlayerRepository

import scala.concurrent.ExecutionContext

object PlayerActor {

  sealed trait Protocol

  case class CreatedResponseWrap(createdResponse: CreatedResponse) extends Protocol

  case class GameSnapshot(
                           roomId: RoomId,
                           masterId: PlayerId,
                           snapshot: Skulking) extends Protocol

  case class JoinSucceeded(
                            roomId: RoomId,
                            masterId: PlayerId,
                            nOfRound: Int,
                            deckId: DeckId,
                            roomMaxSize: Int,
                            players: List[PlayerId]
                          ) extends Protocol

  case class JoinFailed(reason: String) extends Protocol

  case class LookupResponseWrap(lookupResponse: LookupResponse) extends Protocol

  case class ClientProtocolWrapper(clientProtocol: ClientProtocol) extends Protocol

  case class SkulkingEventWrap(skulkingEvent: SkulkingActor.SkulkingEvent) extends Protocol

  case class PlayerCertificated(playerId: PlayerId) extends Protocol

  case class CertificateFailed(reason: String) extends Protocol

  def certificating(out: ActorRef[ServerProtocol], playerRepository: PlayerRepository, roomsActorRef: ActorRef[RoomsActor.Protocol]): Behavior[Protocol] =
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.executionContext

      Behaviors.receiveMessagePartial {
        case ClientProtocolWrapper(clientProtocol) =>
          clientProtocol match {
            case ClientProtocol.Connect(_, sessionId) =>
              context.pipeToSelf(playerRepository.findBy(SessionId(sessionId))) { res =>
                (for {
                  playerIdOr <- res.toEither.left.map(PlayerRepository.UnknownError)
                  playerId <- playerIdOr
                } yield PlayerCertificated(playerId)) match {
                  case Right(certificated) => certificated
                  case Left(e) => CertificateFailed(e.toString)
                }
              }
              Behaviors.same
            case ClientProtocol.Disconnect(_) =>
              Behaviors.stopped
            case _ =>
              Behaviors.unhandled
          }
        case PlayerCertificated(playerId) =>
          out ! ServerProtocol.Connected(playerId = playerId.value)
          apply(playerId, roomsActorRef, out)
        case CertificateFailed(reason) =>
          out ! ServerProtocol.Disconnect(reason = reason)
          Behaviors.same
      }
    }

  def apply(
             playerId: PlayerId, roomsActorRef: ActorRef[RoomsActor.Protocol],
             out: ActorRef[ServerProtocol]): Behavior[Protocol] = {
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial {
        case ClientProtocolWrapper(clientProtocol) =>
          clientProtocol match {
            case ClientProtocol.CreateRoom(_, nOfRound, deckId, roomMaxSize) =>
              val roomCreateResponseAdapter: ActorRef[RoomsActor.CreatedResponse] =
                context.messageAdapter(rsp => CreatedResponseWrap(rsp))
              roomsActorRef ! RoomsActor.Protocol.CreateRoom(
                roomCreateResponseAdapter, playerId, nOfRound,
                DeckId(deckId), roomMaxSize, context.self)

              waitLookup(playerId, roomsActorRef, out)

            case ClientProtocol.Join(_, roomId) =>
              val roomLookupResponseAdapter: ActorRef[RoomsActor.LookupResponse] =
                context.messageAdapter(rsp => LookupResponseWrap(rsp))
              roomsActorRef ! RoomsActor.Protocol.Lookup(roomLookupResponseAdapter, RoomId(roomId))

              waitLookup(playerId, roomsActorRef, out)

            case _ => Behaviors.same
          }
      }
    }
  }

  def waitLookup(
                  playerId: PlayerId, roomsActorRef: ActorRef[RoomsActor.Protocol],
                  out: ActorRef[ServerProtocol]): Behavior[Protocol] = {
    Behaviors.setup { context =>

      Behaviors.receiveMessagePartial {
        case CreatedResponseWrap(CreatedResponse(roomId, skulkingActorRef)) =>
          out ! ServerProtocol.CreateRoomSucceeded(playerId = playerId.value, roomId = roomId.value)
          waiting(playerId, roomId, roomsActorRef, skulkingActorRef, out)

        case LookupResponseWrap(LookupResponse(roomId, skulkingReOpt)) =>
          skulkingReOpt match {
            case Some(skulkingActorRef) =>
              skulkingActorRef ! SkulkingActor.Protocol.Join(playerId, context.self)
              joining(playerId, roomId, roomsActorRef, skulkingActorRef, out)

            case None =>
              out ! ServerProtocol.Disconnect(reason = "部屋が見つかりませんでした")
              disconnecting()
          }
      }
    }
  }

  def joining(playerId: PlayerId, roomId: RoomId,
              roomsActorRef: ActorRef[RoomsActor.Protocol],
              skulkingActorRef: ActorRef[SkulkingActor.Protocol],
              out: ActorRef[ServerProtocol]): Behavior[Protocol] = {
    Behaviors.setup { context =>

      Behaviors.receiveMessagePartial[Protocol] {
        case JoinSucceeded(roomId, masterId, nOfRound, deckId, roomMaxSize, players) =>
          out ! ServerProtocol.JoinSucceeded(
            playerId = playerId.value, masterId = masterId.value, nOfRound = nOfRound,
            deckId = deckId.value, roomMaxSize = roomMaxSize, roomMinSize = 2,
            playerIds = players.map(_.value))
          waiting(playerId, roomId, roomsActorRef, skulkingActorRef, out)

        case JoinFailed(reason) =>
          out ! ServerProtocol.Disconnect(reason = reason)
          disconnecting()

        case SkulkingEventWrap(skulkingEvent) =>
          skulkingEventToServerProtocol(playerId, skulkingEvent).foreach(out.!)
          skulkingEvent match {
            case _: SkulkingEvent.GameStarted =>
              out ! ServerProtocol.Disconnect(reason = "ゲームに参加できませんでした")
              disconnecting()

            case _ =>
              Behaviors.same
          }
      } receiveSignal {
        case (_, PostStop) =>
          skulkingActorRef ! SkulkingActor.Protocol.Leave(playerId)
          Behaviors.same
      }
    }
  }

  def waiting(
               playerId: PlayerId, roomId: RoomId,
               roomsActorRef: ActorRef[RoomsActor.Protocol],
               skulkingActorRef: ActorRef[SkulkingActor.Protocol],
               out: ActorRef[ServerProtocol]): Behavior[Protocol] = {
    Behaviors.setup { context =>

      Behaviors.receiveMessagePartial[Protocol] {
        case ClientProtocolWrapper(clientProtocol) =>
          skulkingActorRef ! clientProtocolToSkulkingProtocol(playerId, context.self, clientProtocol)
          Behaviors.same

        case SkulkingEventWrap(skulkingEvent) =>
          skulkingEventToServerProtocol(playerId, skulkingEvent).foreach(out.!)
          skulkingEvent match {
            case _: SkulkingEvent.GameStarted =>
              started(playerId, roomId, roomsActorRef, skulkingActorRef, out)
            case _ =>
              Behaviors.same
          }
      } receiveSignal {
        case (_, PostStop) =>
          skulkingActorRef ! SkulkingActor.Protocol.Leave(playerId)
          Behaviors.same
      }
    }
  }

  def started(
               playerId: PlayerId, roomId: RoomId,
               roomsActorRef: ActorRef[RoomsActor.Protocol],
               skulkingActorRef: ActorRef[SkulkingActor.Protocol],
               out: ActorRef[ServerProtocol]): Behavior[Protocol] = {
    Behaviors.setup { context =>

      Behaviors.receiveMessagePartial[Protocol] {
        case ClientProtocolWrapper(clientProtocol) =>
          skulkingActorRef ! clientProtocolToSkulkingProtocol(playerId, context.self, clientProtocol)
          Behaviors.same

        case SkulkingEventWrap(skulkingEvent) =>
          skulkingEventToServerProtocol(playerId, skulkingEvent).foreach(out.!)
          skulkingEvent match {
            case SkulkingEvent.ShutdownGame() =>
              Behaviors.stopped(
                () => roomsActorRef ! RoomsActor.Protocol.DeleteRoom(roomId))
            case _ =>
              Behaviors.same
          }
      } receiveSignal {
        case (_, PostStop) =>
          skulkingActorRef ! SkulkingActor.Protocol.DeActive(playerId)
          Behaviors.same
      }
    }
  }

  def disconnecting(): Behavior[Protocol] = {
    Behaviors.receiveMessagePartial {
      case ClientProtocolWrapper(Disconnect(_)) =>
        Behaviors.stopped
    }
  }

  private def clientProtocolToSkulkingProtocol(
                                                playerId: PlayerId,
                                                playerActorRef: ActorRef[Protocol],
                                                input: ClientProtocol): SkulkingActor.Protocol =
    input match {
      case ClientProtocol.Leave(_) =>
        SkulkingActor.Protocol.Leave(playerId)
      case ClientProtocol.Start(_) =>
        SkulkingActor.Protocol.Start(playerId)
      case ClientProtocol.BidDeclare(_, number) =>
        SkulkingActor.Protocol.BidDeclareChange(playerId, number)
      case ClientProtocol.PlayTrick(_, cardId) =>
        SkulkingActor.Protocol.PlayTrick(playerId, CardId(cardId))
      case ClientProtocol.PlayTigress(_, cardId, isPirates) =>
        SkulkingActor.Protocol.PlayTigress(playerId, CardId(cardId), isPirates)
      case ClientProtocol.PlayRascalOfRoatan(_, cardId, betScore) =>
        SkulkingActor.Protocol.PlayRascalOfRoatan(playerId, CardId(cardId), betScore)
      case ClientProtocol.ChangeDealer(_, newDealerId) =>
        SkulkingActor.Protocol.ChangeDealer(playerId, PlayerId(newDealerId))
      case ClientProtocol.ChangeHand(_, returnCards) =>
        SkulkingActor.Protocol.ChangeHand(PlayerId(playerId.value), returnCards.map(CardId))
      case ClientProtocol.FuturePredicateFinish(_) =>
        SkulkingActor.Protocol.FuturePredicateFinish(playerId)
      case ClientProtocol.BidDeclareChange(_, changedBid) =>
        SkulkingActor.Protocol.BidDeclareChange(playerId, changedBid)
      case ClientProtocol.Replay(_) =>
        SkulkingActor.Protocol.Replay
      case ClientProtocol.Finish(_) =>
        SkulkingActor.Protocol.Finish
      case _ => SkulkingActor.Protocol.Finish
    }

  private def skulkingEventToServerProtocol(playerId: PlayerId, skulkingEvent: SkulkingEvent): Option[ServerProtocol] =
    skulkingEvent match {
      case SkulkingEvent.NewPlayerJoined(pId) =>
        Some(ServerProtocol.NewPlayerJoined(playerId = pId.value))
      case SkulkingEvent.MasterLeave(leaveMater, nextMaster) =>
        Some(ServerProtocol.MasterLeave(leaveMater = leaveMater.value, nextMaster = nextMaster.value))
      case SkulkingEvent.PlayerLeave(pId) =>
        Some(ServerProtocol.PlayerLeave(playerId = pId.value))
      case SkulkingEvent.GameStarted() =>
        Some(ServerProtocol.GameStarted())
      case SkulkingEvent.NewRoundStarted(round, hands, restOfDeck) =>
        for {
          hand <- hands.get(playerId)
        } yield ServerProtocol.NewRoundStarted(round = round, hands = hand.map(_.value).toList, restOfDeck = restOfDeck)
      case SkulkingEvent.BidDeclared(pId) =>
        Some(ServerProtocol.BidDeclared(playerId = pId.value))
      case SkulkingEvent.TrickPhaseStarted(bidDeclares) =>
        Some(ServerProtocol.TrickPhaseStarted(
          bidDeclares = bidDeclares.map(declare => (declare.playerId.value, declare.declareBid))))
      case SkulkingEvent.Played(pId, cardId) =>
        Some(ServerProtocol.Played(playerId = pId.value, cardId = cardId.value))
      case SkulkingEvent.TigressPlayed(pId, cardId, isPirates) =>
        Some(ServerProtocol.TigressPlayed(playerId = pId.value, cardId = cardId.value, isPirates = isPirates))
      case SkulkingEvent.RascalOfRoatanPlayed(pId, cardId, betScore) =>
        Some(ServerProtocol.RascalOfRoatanPlayed(playerId = pId.value, cardId = cardId.value, betScore = betScore))
      case SkulkingEvent.TrickFinished(trickWinner) =>
        Some(ServerProtocol.TrickFinished(trickWinner = trickWinner.value))
      case SkulkingEvent.PiratesEventWrapper(piratesEvent) =>
        piratesEvent match {
          case PiratesEvent.NoEvent => None
          case PiratesEvent.DeclareBidChangeAvailable(pId, min, max) =>
            if (pId == playerId) Some(ServerProtocol.DeclareBidChangeAvailableNotice(playerId = pId.value, min = min, max = max))
            else Some(ServerProtocol.DeclareBidChangingNotice(playerId = pId.value))
          case PiratesEvent.FuturePredicated(pId, deckCard) =>
            if (pId == playerId) Some(ServerProtocol.FuturePredicated(deckCard = deckCard.map(_.value)))
            else Some(ServerProtocol.FuturePredicatedOtherPlayer(playerId = pId.value))
          case PiratesEvent.GotBonusScore(pId, bonusScore) =>
            Some(ServerProtocol.GotBonusScore(playerId = pId.value, bonusScore = bonusScore))
          case PiratesEvent.HandChangeAvailableNotice(pId, drawCard) =>
            if (pId == playerId) Some(ServerProtocol.HandChangeAvailableNotice(drawCards = drawCard.map(_.value)))
            else Some(ServerProtocol.HandChangingNotice(changingplayerId = pId.value))
          case PiratesEvent.NextTrickLeadPlayerChangeableNotice(pId) =>
            if (pId == playerId) Some(ServerProtocol.NextTrickLeadPlayerChangeableNotice())
            else Some(ServerProtocol.NextTrickLeadPlayerChangingNotice(changingplayerId = pId.value))
        }
      case SkulkingEvent.FuturePredicateFinished(playerId) =>
        Some(ServerProtocol.FuturePredicateFinished(playerId = playerId.value))
      case SkulkingEvent.BidDeclareChanged(playerId, changeBid) =>
        Some(ServerProtocol.DeclareBidChanged(playerId = playerId.value, changedBid = changeBid))
      case SkulkingEvent.NextPlay(nextDealerId) =>
        Some(ServerProtocol.NextPlay(nextPlayerId = nextDealerId.value))
      case SkulkingEvent.RoundFinished(round, roundScore) =>
        Some(ServerProtocol.RoundFinished(round = round, roundScore = roundScore.scores.map(s => s.copy(_1 = s._1.value))))
      case SkulkingEvent.GameFinished(gameWinner, gameScore) =>
        Some(ServerProtocol.GameFinished(gameWinner = gameWinner.value, gameScore = gameScore.map(s => s.copy(_1 = s._1.value))))
      case SkulkingEvent.NextTrickChanged(from, to) =>
        Some(ServerProtocol.NextTrickChanged(from = from.value, to = to.value))
      case SkulkingEvent.HandChanged(changed, nOfChanged) =>
        Some(ServerProtocol.HandChanged(changed = changed.value, nOfChanged = nOfChanged))
      case SkulkingEvent.ReplayGame() =>
        Some(ServerProtocol.ReplayGame())
      case SkulkingEvent.ShutdownGame() =>
        Some(ServerProtocol.ShutdownGame())
      case _ => None
    }

}
