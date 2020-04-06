package client.scene

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SpawnProtocol}
import jp.skulking.domain._
import jp.skulking.protocol.{ClientProtocol, ClientProtocolJSONFormats, ServerProtocol, ServerProtocolJSONFormats}
import org.scalajs.dom.{Event, MessageEvent, WebSocket}
import play.api.libs.json.Json

object SkulkingBoardSceneActor {

  private val WEB_SOCKET_ENDPOINT = "ws://localhost:9000/connect"
  private val clientProtocolJSONFormats = new ClientProtocolJSONFormats {}
  private val serverProtocolJSONFormats = new ServerProtocolJSONFormats {}

  def supervisor(): Behavior[SpawnProtocol.Command] =
    Behaviors.setup { context =>

      SpawnProtocol()
    }

  def createWebSocket(self: ActorRef[ServerProtocol], sessionId: SessionId): WebSocket = {
    val webSocket = new WebSocket(WEB_SOCKET_ENDPOINT)

    webSocket.onopen = (event: Event) => {
      val connectMessage = ClientProtocol.Connect(sessionId = sessionId.value)
      val serialized = clientProtocolJSONFormats.serialize(connectMessage).toString()
      webSocket.send(serialized)
    }

    webSocket.onerror = (event: Event) => {
      self ! ServerProtocol.Disconnected(reason = event.toString)
    }

    webSocket.onmessage = (event: MessageEvent) => {
      val deSerialized = serverProtocolJSONFormats.deserialize(Json.parse(event.data.asInstanceOf[String]))
      self ! deSerialized
    }

    webSocket
  }

  def createRoom(frameId: FrameId, sessionId: SessionId, nOfRound: Int, deckId: DeckId, roomMaxSize: Int = 6): Behavior[ServerProtocol] = {
    Behaviors.setup { context =>
      val webSocket = createWebSocket(context.self, sessionId)
      val ref = context.spawnAnonymous(clientProtocolActor(webSocket))

      Behaviors.receiveMessagePartial {
        case ServerProtocol.Connected(_, _) =>
          ref ! ClientProtocol.CreateRoom(nOfRound = nOfRound, deckId = deckId.value, roomMaxSize = roomMaxSize)
          Behaviors.same

        case ServerProtocol.CreateRoomSucceeded(_, playerId, roomId) =>
          val skulkingBoardEntrance =
            SkulkingBoardEntrance(
              sessionId, frameId, RoomId(roomId), PlayerId(playerId), nOfRound, CardDecks.decks(deckId),
              roomMaxSize, 2, PlayerId(playerId), PlayerId(playerId) :: Nil, ref)

          frameId.transition(skulkingBoardEntrance.self)
          entrance(frameId, sessionId, ref, skulkingBoardEntrance)

        case ServerProtocol.Disconnected(_, reason) =>
          RoomsEntrance.transition(frameId, sessionId)
          Behaviors.stopped

        case ServerProtocol.Disconnect(_, reason) =>
          RoomsEntrance.transition(frameId, sessionId)
          ref ! ClientProtocol.Disconnect()
          Behaviors.stopped
      }
    }
  }

  def joinRoom(frameId: FrameId, sessionId: SessionId, roomId: RoomId): Behavior[ServerProtocol] = {
    Behaviors.setup { context =>
      val webSocket = createWebSocket(context.self, sessionId)
      val ref = context.spawnAnonymous(clientProtocolActor(webSocket))

      Behaviors.receiveMessagePartial {
        case ServerProtocol.Connected(_, _) =>
          ref ! ClientProtocol.Join(roomId = roomId.value)
          Behaviors.same

        case ServerProtocol.JoinSucceeded(_, playerId, masterId, nOfRound, deckId, roomMaxSize, roomMinSize, playerIds) =>
          val skulkingBoardEntrance =
            SkulkingBoardEntrance(
              sessionId, frameId, roomId, PlayerId(playerId), nOfRound, CardDecks.decks(DeckId(deckId)),
              roomMaxSize, roomMinSize, PlayerId(masterId), playerIds.map(PlayerId), ref)

          frameId.transition(skulkingBoardEntrance.self)
          if (playerId == masterId) {
            skulkingBoardEntrance.reconnected(PlayerId(playerId))
          }
          entrance(frameId, sessionId, ref, skulkingBoardEntrance)

        case ServerProtocol.Disconnected(_, reason) =>
          RoomsEntrance.transition(frameId, sessionId)
          Behaviors.stopped

        case ServerProtocol.Disconnect(_, reason) =>
          RoomsEntrance.transition(frameId, sessionId)
          ref ! ClientProtocol.Disconnect()
          Behaviors.stopped

      }
    }
  }

  def entrance(frameId: FrameId, sessionId: SessionId, clientProtocolActorRef: ActorRef[ClientProtocol], skulkingBoardEntrance: SkulkingBoardEntrance): Behavior[ServerProtocol] =
    Behaviors.setup { context =>

      Behaviors.receiveMessagePartial {
        case ServerProtocol.NewPlayerJoined(_, playerId) =>
          skulkingBoardEntrance.newPlayerJoined(PlayerId(playerId))
          Behaviors.same

        case ServerProtocol.MasterLeave(_, leaveMater, nextMaster) =>
          // TODO: appropriate handling
          skulkingBoardEntrance.playerLeave(PlayerId(leaveMater))
          Behaviors.same

        case ServerProtocol.PlayerLeave(_, playerId) =>
          skulkingBoardEntrance.playerLeave(PlayerId(playerId))
          Behaviors.same

        case ServerProtocol.GameStarted(_) =>
          val skulkingBoard = skulkingBoardEntrance.gameStarted()
          frameId.transition(skulkingBoard.self)

          board(frameId, sessionId, clientProtocolActorRef, skulkingBoard)

        case ServerProtocol.Disconnected(_, reason) =>
          RoomsEntrance.transition(frameId, sessionId)
          Behaviors.stopped

        case ServerProtocol.Disconnect(_, reason) =>
          RoomsEntrance.transition(frameId, sessionId)
          clientProtocolActorRef ! ClientProtocol.Disconnect()
          Behaviors.stopped
      }
    }

  def board(frameId: FrameId, sessionId: SessionId, clientProtocolActorRef: ActorRef[ClientProtocol], skulkingBoard: SkulkingBoard): Behavior[ServerProtocol] =
    Behaviors.setup { context =>
      Behaviors.receiveMessagePartial {
        case ServerProtocol.NewRoundStarted(_, round, hands, restOfDeck) =>
          skulkingBoard.newRoundStarted(round, hands.map(CardId), restOfDeck)
          Behaviors.same

        case ServerProtocol.BidDeclared(_, playerId) =>
          skulkingBoard.bidDeclared(PlayerId(playerId))
          Behaviors.same

        case ServerProtocol.TrickPhaseStarted(_, bidDeclares) =>
          skulkingBoard.trickPhaseStarted(bidDeclares.map(d => (PlayerId(d._1), d._2)).toMap)
          Behaviors.same

        case ServerProtocol.Played(_, playerId, cardId) =>
          skulkingBoard.played(PlayerId(playerId), CardId(cardId))
          Behaviors.same

        case ServerProtocol.TigressPlayed(_, playerId, cardId, isPirates) =>
          skulkingBoard.tigressPlayed(PlayerId(playerId), CardId(cardId), isPirates)
          Behaviors.same

        case ServerProtocol.RascalOfRoatanPlayed(_, playerId, cardId, betScore) =>
          skulkingBoard.rascalOfRoatanPlayed(PlayerId(playerId), CardId(cardId), betScore)
          Behaviors.same

        case ServerProtocol.TrickFinished(_, trickWinner) =>
          skulkingBoard.trickFinished(PlayerId(trickWinner))
          Behaviors.same

        case ServerProtocol.NextTrickLeadPlayerChangeableNotice(_) =>
          skulkingBoard.nextTrickLeadPlayerChangeableNotice()
          Behaviors.same

        case ServerProtocol.NextTrickLeadPlayerChangingNotice(_, changingplayerId) =>
          skulkingBoard.nextTrickLeadPlayerChangingNotice(PlayerId(changingplayerId))
          Behaviors.same

        case ServerProtocol.HandChangeAvailableNotice(_, drawCards) =>
          skulkingBoard.handChangeAvailableNotice(drawCards.map(CardId))
          Behaviors.same

        case ServerProtocol.HandChangingNotice(_, changingplayerId) =>
          skulkingBoard.handChangingNotice(PlayerId(changingplayerId))
          Behaviors.same

        case ServerProtocol.GotBonusScore(_, playerId, bonusScore) =>
          skulkingBoard.gotBonusScore(PlayerId(playerId), bonusScore)
          Behaviors.same

        case ServerProtocol.FuturePredicated(_, deckCard) =>
          skulkingBoard.futurePredicated(deckCard.map(CardId))
          Behaviors.same

        case ServerProtocol.FuturePredicatedOtherPlayer(_, playerId) =>
          skulkingBoard.futurePredicatedOtherPlayer(PlayerId(playerId))
          Behaviors.same

        case ServerProtocol.FuturePredicateFinished(_, playerId) =>
          skulkingBoard.futurePredicateFinished(PlayerId(playerId))
          Behaviors.same

        case ServerProtocol.DeclareBidChangeAvailableNotice(_, playerId, min, max) =>
          skulkingBoard.declareBidChange(min, max)
          Behaviors.same

        case ServerProtocol.DeclareBidChangingNotice(_, playerId) =>
          skulkingBoard.declareBidChanging(PlayerId(playerId))
          Behaviors.same

        case ServerProtocol.DeclareBidChanged(_, playerId, changedBid) =>
          skulkingBoard.declareBidChanged(PlayerId(playerId), changedBid)
          Behaviors.same

        case ServerProtocol.NextPlay(_, nextPlayerId) =>
          skulkingBoard.nextPlay(PlayerId(nextPlayerId))
          Behaviors.same

        case ServerProtocol.RoundFinished(_, round, roundScore) =>
          skulkingBoard.roundFinished(round, roundScore.map(s => (PlayerId(s._1), s._2)).toMap)
          Behaviors.same

        case ServerProtocol.GameFinished(_, gameWinner, gameScore) =>
          skulkingBoard.gameFinished(PlayerId(gameWinner), gameScore.map(s => (PlayerId(s._1), s._2)))
          Behaviors.same

        case ServerProtocol.NextTrickChanged(_, from, to) =>
          skulkingBoard.nextTrickChanged(PlayerId(from), PlayerId(to))
          Behaviors.same

        case ServerProtocol.HandChanged(_, changed, nOfChanged) =>
          skulkingBoard.handChanged(PlayerId(changed), nOfChanged)
          Behaviors.same

        case ServerProtocol.ReplayGame(_) =>
          skulkingBoard.replayGame()
          Behaviors.same

        case ServerProtocol.ShutdownGame(_) =>
          skulkingBoard.shutdownGame()
          Behaviors.stopped

        case ServerProtocol.Disconnected(_, reason) =>
          RoomsEntrance.transition(frameId, sessionId)
          Behaviors.stopped

        case ServerProtocol.Disconnect(_, reason) =>
          RoomsEntrance.transition(frameId, sessionId)
          clientProtocolActorRef ! ClientProtocol.Disconnect()
          Behaviors.stopped
      }
    }

  def clientProtocolActor(webSocket: WebSocket): Behavior[ClientProtocol] =
    Behaviors.receiveMessage { protocol =>
      val serialized = clientProtocolJSONFormats.serialize(protocol).toString()
      webSocket.send(serialized)
      Behaviors.same
    }

}
