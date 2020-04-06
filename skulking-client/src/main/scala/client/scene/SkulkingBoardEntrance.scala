package client.scene

import akka.actor.typed.ActorRef
import client.component.{EventStreamComponent, PlayersComponent, PopUpInterfaceComponent}
import jp.skulking.domain.Skulking.Deck
import jp.skulking.domain.{PlayerId, RoomId, SessionId}
import jp.skulking.protocol.ClientProtocol
import org.scalajs.dom.html.Div
import scalatags.JsDom.all._

final case class SkulkingBoardEntrance private(
                                                sessionId: SessionId,
                                                frameId: FrameId,
                                                roomId: RoomId,
                                                myPlayerId: PlayerId,
                                                nOfRounds: Int,
                                                deck: Deck,
                                                roomMaxSize: Int,
                                                roomMinSize: Int,
                                                private var masterId: PlayerId,
                                                private var playerIds: List[PlayerId],
                                                clientProtocolActorRef: ActorRef[ClientProtocol]) {

  private[this] final val popUpInterfaceComponent = PopUpInterfaceComponent(frameId.id)
  private[this] final val playersComponent = PlayersComponent(frameId.id, myPlayerId)
  private[this] final val eventStreamComponent = EventStreamComponent(frameId.id)

  val self: Div =
    div(
      id := frameId.id,
      popUpInterfaceComponent.self,
      div(
        cls := "skulking-container-dashboard",
        div(
          cls := "skulking-container-center",
          div(
            playersComponent.self,
            eventStreamComponent.self
          )
        )
      )
    ).render
  playerIds.foreach(playersComponent.join)

  def reconnected(playerId: PlayerId): Unit = {
    if (playerIds.size >= roomMinSize && masterId == myPlayerId) {
      popUpInterfaceComponent.yesPopup(
        s"ゲームの開始最低人数を超えました ゲームを開始しますか?", "はい",
        {
          clientProtocolActorRef ! ClientProtocol.Start()
        }
      )
    }
  }

  def newPlayerJoined(playerId: PlayerId): Unit = {
    playerIds = playerIds :+ playerId
    playersComponent.join(playerId)
    if (playerIds.size >= roomMinSize && masterId == myPlayerId) {
      popUpInterfaceComponent.yesPopup(
        s"ゲームの開始最低人数を超えました ゲームを開始しますか?", "はい",
        {
          clientProtocolActorRef ! ClientProtocol.Start()
        }
      )
    }
    eventStreamComponent.consume(s"新しいプレイヤー ${playerId.value}が参加しました")
  }

  def playerLeave(playerId: PlayerId): Unit = {
    playerIds = playerIds.filter(_ != playerId)
    playersComponent.leave(playerId)
    if (playerIds.size < roomMinSize && masterId == myPlayerId) {
      popUpInterfaceComponent.clear()
    }
    eventStreamComponent.consume(s"プレイヤー ${playerId.value}が退出しました")
  }

  def gameCantStart(reason: String): Unit = {
    eventStreamComponent.consume(s"${reason}のためゲームを開始できませんでした")
  }

  def gameStarted(): SkulkingBoard = {
    eventStreamComponent.consume("新しいゲームが開始されました")
    SkulkingBoard.transition(
      frameId = frameId,
      sessionId = sessionId,
      myPlayerId = myPlayerId,
      nOfRounds = nOfRounds,
      deck = deck,
      roomMaxSize = roomMaxSize,
      roomMinSize = roomMinSize,
      masterId = masterId,
      playerIds = playerIds,
      popUpInterfaceComponent = popUpInterfaceComponent,
      playersComponent = playersComponent,
      eventStreamComponent = eventStreamComponent,
      clientProtocolActorRef = clientProtocolActorRef)
  }

}
