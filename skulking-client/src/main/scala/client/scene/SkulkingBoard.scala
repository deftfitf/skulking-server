package client.scene

import akka.actor.typed.ActorRef
import client.component._
import jp.skulking.domain.Card.{RascalOfRoatan, Tigress}
import jp.skulking.domain.Skulking.Deck
import jp.skulking.domain.{CardId, PlayerId, SessionId}
import jp.skulking.protocol.ClientProtocol
import org.scalajs.dom.html.Div
import scalatags.JsDom.all._

final case class SkulkingBoard private(
                                        frameId: FrameId,
                                        sessionId: SessionId,
                                        myPlayerId: PlayerId,
                                        nOfRounds: Int,
                                        deck: Deck,
                                        roomMaxSize: Int,
                                        roomMinSize: Int,
                                        masterId: PlayerId,
                                        playerIds: List[PlayerId],
                                        popUpInterfaceComponent: PopUpInterfaceComponent,
                                        playersComponent: PlayersComponent,
                                        eventStreamComponent: EventStreamComponent,
                                        clientProtocolActorRef: ActorRef[ClientProtocol]) {

  private[this] final val infoComponent = InfoComponent(frameId.id, nOfRounds, deck, roomMaxSize, roomMinSize)
  private[this] final val handsComponent = HandsComponent(frameId.id, deck)
  private[this] final val fieldComponent = FieldComponent(frameId.id, deck)
  private[this] final val scoreBoardComponent = ScoreBoardComponent(frameId.id, playerIds)

  val self: Div =
    div(
      id := frameId.id,
      popUpInterfaceComponent.self,
      div(
        cls := "skulking-container",
        div(
          cls := "skulking-container-left",
          infoComponent.self,
          handsComponent.self,
          fieldComponent.self,
          scoreBoardComponent.self
        ),
        div(
          cls := "skulking-container-right",
          playersComponent.self,
          eventStreamComponent.self
        )
      )
    ).render

  def newRoundStarted(round: Int, hands: List[CardId], restOfDeck: Int): Unit = {
    infoComponent.updateRound(round)
    playersComponent.newRoundStarted(hands.length)
    handsComponent.drawCards(hands, restOfDeck)
    fieldComponent.showBidCards(round, bid => {
      popUpInterfaceComponent.yesNoPopup(
        s"ビッドを${bid}で宣言しますか?", "はい", "いいえ",
        {
          fieldComponent.removeExcept(bid)
          clientProtocolActorRef ! ClientProtocol.BidDeclare(number = bid)
        }, {}
      )
    })
    eventStreamComponent.consume("新しいラウンドが開始されました")
  }

  def bidDeclared(playerId: PlayerId): Unit = {
    playersComponent.bidDeclared(playerId)
    eventStreamComponent.consume(s"${playerId.value}がビッドを確定しました")
  }

  def trickPhaseStarted(bidDeclares: Map[PlayerId, Int]): Unit = {
    playersComponent.trickPhaseStarted(bidDeclares)
    eventStreamComponent.consume(s"全てのビッドが出揃いました 新しいトリックが開始されます")
  }

  def nextPlay(playerId: PlayerId): Unit = {
    playersComponent.nextPlay(playerId)
    if (playerId == myPlayerId) {
      playCardPopup()
    }
    eventStreamComponent.consume(s"次のトリックは${playerId.value}です")
  }

  private def playCardPopup(): Unit = {
    handsComponent.playMyTurn(fieldComponent.getField, (card, doneSelect) => {
      card match {
        case _: Tigress =>
          popUpInterfaceComponent.multiSelectPopup(
            s"${card.id.value}をプレイしますか?",
            ("海賊でプレイ", () => {
              doneSelect()
              clientProtocolActorRef ! ClientProtocol.PlayTigress(cardId = card.id.value, isPirates = true)
            }),
            ("逃亡でプレイ", () => {
              doneSelect()
              clientProtocolActorRef ! ClientProtocol.PlayTigress(cardId = card.id.value, isPirates = false)
            }),
            ("キャンセル", () => {}))

        case _: RascalOfRoatan =>
          popUpInterfaceComponent.multiSelectPopup(
            s"${card.id.value}をプレイしますか?",
            ("賭けない", () => {
              doneSelect()
              clientProtocolActorRef ! ClientProtocol.PlayTrick(cardId = card.id.value)
            }),
            ("10スコア賭ける", () => {
              doneSelect()
              clientProtocolActorRef ! ClientProtocol.PlayRascalOfRoatan(cardId = card.id.value, betScore = 10)
            }),
            ("20スコア賭ける", () => {
              doneSelect()
              clientProtocolActorRef ! ClientProtocol.PlayRascalOfRoatan(cardId = card.id.value, betScore = 20)
            }),
            ("キャンセル", () => {}))
        case _ =>
          popUpInterfaceComponent.yesNoPopup(
            s"${card.id.value}をプレイしますか?", "はい", "キャンセル",
            {
              doneSelect()
              clientProtocolActorRef ! ClientProtocol.PlayTrick(cardId = card.id.value)
            }, {})
      }
    })
  }

  def played(playerId: PlayerId, cardId: CardId): Unit = {
    playersComponent.played(playerId)
    fieldComponent.played(playerId, cardId)
    eventStreamComponent.consume(s"${playerId.value}がプレイしました")
  }

  def tigressPlayed(playerId: PlayerId, cardId: CardId, isPirates: Boolean): Unit = {
    playersComponent.played(playerId)
    fieldComponent.tigressPlayed(playerId, cardId, isPirates)
    eventStreamComponent.consume(s"${playerId.value}がティグレスを${if (isPirates) "海賊" else "逃走"}でプレイしました")
  }

  def rascalOfRoatanPlayed(playerId: PlayerId, cardId: CardId, betScore: Int): Unit = {
    playersComponent.played(playerId)
    fieldComponent.played(playerId, cardId)
    eventStreamComponent.consume(s"${playerId.value}がラスカルの能力で${betScore}スコアをかけました")
  }

  def trickFinished(trickWinner: PlayerId): Unit = {
    playersComponent.trickFinished(trickWinner)
    fieldComponent.clear()
    eventStreamComponent.consume(s"${trickWinner.value}がトリックを制しました")
  }

  def nextTrickLeadPlayerChangeableNotice(): Unit = {
    playersComponent.nextTrickLeadPlayerChange((playerId, doneSelect) => {
      popUpInterfaceComponent.yesNoPopup(
        s"次のトリックのリードプレイヤーを${playerId.value}に変更しますか?", "はい", "キャンセル",
        {
          doneSelect()
          clientProtocolActorRef ! ClientProtocol.ChangeDealer(newDealerId = playerId.value)
        }, {})
    })
    eventStreamComponent.consume(s"${myPlayerId.value}が次のリードプレイヤーを選択しています")
  }

  def nextTrickLeadPlayerChangingNotice(changingPlayerId: PlayerId): Unit = {
    playersComponent.playerThinking(changingPlayerId)
    eventStreamComponent.consume(s"${changingPlayerId.value}が次のプレイのリードプレイヤーを選択しています")
  }

  def nextTrickChanged(from: PlayerId, to: PlayerId): Unit = {
    playersComponent.playerThinkingDone(from)
    eventStreamComponent.consume(s"${from.value}によって次のプレイのリードプレイヤーが${to.value}に変更されました")
  }

  def handChangeAvailableNotice(drawCards: List[CardId]): Unit = {
    handsComponent.drawCards(drawCards)
    handsComponent.selectableCards(
      drawCards.size,
      popUpInterfaceComponent,
      selected => {
        clientProtocolActorRef ! ClientProtocol.ChangeHand(returnCards = selected.map(_.value))
      })
    eventStreamComponent.consume(s"バイジの力で山札から${drawCards.size}引き交換することができます")
  }

  def handChangingNotice(changingPlayerId: PlayerId): Unit = {
    playersComponent.playerThinking(changingPlayerId)
    eventStreamComponent.consume(s"${changingPlayerId.value}が手札を変更しています")
  }

  def handChanged(changingPlayerId: PlayerId, nOfChanged: Int): Unit = {
    playersComponent.playerThinkingDone(changingPlayerId)
    eventStreamComponent.consume(s"${changingPlayerId.value}が手札を${nOfChanged}枚変更しました")
  }

  def gotBonusScore(playerId: PlayerId, bonusScore: Int): Unit = {
    eventStreamComponent.consume(s"${playerId.value}がボーナススコアを${bonusScore}点獲得しました")
  }

  def futurePredicated(deckCard: List[CardId]): Unit = {
    fieldComponent.showDeckCards(deckCard)
    popUpInterfaceComponent.yesPopup(
      "山札のカードを見るのを終了しますか?", "はい",
      {
        fieldComponent.clear()
        clientProtocolActorRef ! ClientProtocol.FuturePredicateFinish()
      }
    )
    eventStreamComponent.consume(s"${myPlayerId.value}が山札を閲覧しています")
  }

  def futurePredicatedOtherPlayer(playerId: PlayerId): Unit = {
    playersComponent.playerThinking(playerId)
    eventStreamComponent.consume(s"${playerId.value}が山札を閲覧しています")
  }

  def futurePredicateFinished(playerId: PlayerId): Unit = {
    playersComponent.playerThinkingDone(playerId)
    eventStreamComponent.consume(s"${playerId.value}が山札を閲覧し終わりました")
  }

  def declareBidChange(min: Int, max: Int): Unit = {
    popUpInterfaceComponent.rangeButtonPopup(
      "変更するビッドの値を選択してください", min, max,
      bid => {
        clientProtocolActorRef ! ClientProtocol.BidDeclareChange(changedBid = bid)
      }
    )
    eventStreamComponent.consume(s"${myPlayerId.value}がビッドを変更しています")
  }

  def declareBidChanging(playerId: PlayerId): Unit = {
    playersComponent.playerThinking(playerId)
    eventStreamComponent.consume(s"${playerId.value}がビッドを変更しています")
  }

  def declareBidChanged(playerId: PlayerId, changedBid: Int): Unit = {
    playersComponent.playerThinkingDone(playerId)
    playersComponent.setBid(playerId, changedBid)
    eventStreamComponent.consume(s"${playerId.value}がビッドを変更し終わりました")
  }

  def roundFinished(round: Int, roundScore: Map[PlayerId, (Int, Int)]): Unit = {
    scoreBoardComponent.roundFinished(round, roundScore)
    eventStreamComponent.consume(s"ラウンドが終了しました")
  }

  def gameFinished(gameWinner: PlayerId, gameScore: Map[PlayerId, Int]): Unit = {
    scoreBoardComponent.gameFinished(gameScore)
    if (masterId == myPlayerId) {
      popUpInterfaceComponent.yesNoPopup(
        "ゲームを再度プレイしますか?", "はい", "終了",
        {
          clientProtocolActorRef ! ClientProtocol.Replay()
        }, {
          clientProtocolActorRef ! ClientProtocol.Finish()
        })
    }
    eventStreamComponent.consume(s"ゲームが終了しました 勝者は${gameWinner.value}です")
  }

  def replayGame(): Unit = {
    infoComponent.updateRound(0)
    fieldComponent.clear()
    scoreBoardComponent.clear()
    eventStreamComponent.consume(s"ゲームをリプレイすることが選択されました")
  }

  def shutdownGame(): Unit = {
    eventStreamComponent.consume(s"ゲームを終了します")
    RoomsEntrance.transition(frameId, sessionId)
  }

}

object SkulkingBoard {

  def transition(
                  frameId: FrameId,
                  sessionId: SessionId,
                  myPlayerId: PlayerId,
                  nOfRounds: Int,
                  deck: Deck,
                  roomMaxSize: Int,
                  roomMinSize: Int,
                  masterId: PlayerId,
                  playerIds: List[PlayerId],
                  popUpInterfaceComponent: PopUpInterfaceComponent,
                  playersComponent: PlayersComponent,
                  eventStreamComponent: EventStreamComponent,
                  clientProtocolActorRef: ActorRef[ClientProtocol]): SkulkingBoard = {
    SkulkingBoard(
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