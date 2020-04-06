package client.component

import client.{HtmlClassDef, jquery}
import jp.skulking.domain.PlayerId
import org.scalajs.dom.Element
import org.scalajs.dom.html.Div
import org.scalajs.jquery.JQueryEventObject
import scalatags.JsDom.all._

final case class PlayersComponent(
                                   parentId: String, myPlayerId: PlayerId) {
  private val elementId = s"$parentId-players"
  val self: Div = div(id := elementId).render

  import scala.collection.mutable

  private[this] val playerToElement = mutable.Map.empty[PlayerId, Div]
  private[this] val playerToNOfHands = mutable.Map.empty[PlayerId, Int]
  private[this] val playerToNOfTakenTrick = mutable.Map.empty[PlayerId, Int]

  private def playerElementId(playerId: PlayerId): String =
    s"$elementId-${playerId.value}"

  private def playerDivFormat(playerId: PlayerId): Div =
    div(
      id := playerElementId(playerId),
      p(playerId.value),
      ol(
        li(cls := HtmlClassDef.PLAYER_STATUS_THINKING),
        li(cls := HtmlClassDef.PLAYER_STATUS_BID_DECLARED),
        li(cls := HtmlClassDef.PLAYER_N_OF_HANDS),
        li(cls := HtmlClassDef.DECLARED_BID_N),
        li(cls := HtmlClassDef.PLAYER_TAKEN_TRICK)
      )
    ).render

  def join(playerId: PlayerId): Unit = {
    val playerDiv = playerDivFormat(playerId)
    jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_THINKING).hide()
    jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_BID_DECLARED).hide()
    jquery(playerDiv).find("." + HtmlClassDef.PLAYER_N_OF_HANDS).hide()
    jquery(playerDiv).find("." + HtmlClassDef.DECLARED_BID_N).hide()
    jquery(playerDiv).find("." + HtmlClassDef.PLAYER_TAKEN_TRICK).hide()

    playerToElement.put(playerId, playerDiv)
    jquery(self).append(playerDiv)
  }

  def leave(playerId: PlayerId): Unit = {
    playerToElement
      .get(playerId)
      .foreach(playerDiv => {
        playerToElement.remove(playerId)
        jquery(playerDiv).remove()
      })
  }

  def newRoundStarted(nOfHands: Int): Unit = {
    for {
      playerId <- playerToElement.keys
      playerDiv <- playerToElement.get(playerId)
    } {
      jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_BID_DECLARED).hide()
      jquery(playerDiv)
        .find("." + HtmlClassDef.PLAYER_TAKEN_TRICK)
        .replaceWith(li(cls := HtmlClassDef.PLAYER_TAKEN_TRICK, 0).render)
        .show()

      playerToNOfHands.put(playerId, nOfHands)
      playerToNOfTakenTrick.put(playerId, 0)
      jquery(playerDiv)
        .find("." + HtmlClassDef.PLAYER_N_OF_HANDS)
        .replaceWith(li(cls := HtmlClassDef.PLAYER_N_OF_HANDS, nOfHands).render)
        .show()
    }
  }

  def bidDeclared(playerId: PlayerId): Unit = {
    playerToElement
      .get(playerId)
      .foreach(playerDiv => {
        jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_BID_DECLARED).show()
      })
  }

  def trickPhaseStarted(bidDeclares: Map[PlayerId, Int]): Unit = {
    for {
      bidDeclare <- bidDeclares
      playerDiv <- playerToElement.get(bidDeclare._1)
    } {
      jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_BID_DECLARED).hide()
      jquery(playerDiv)
        .find("." + HtmlClassDef.DECLARED_BID_N)
        .replaceWith(li(cls := HtmlClassDef.DECLARED_BID_N, bidDeclare._2).render)
        .show()
    }
  }

  def setBid(playerId: PlayerId, bid: Int): Unit = {
    for {
      playerDiv <- playerToElement.get(playerId)
    } {
      jquery(playerDiv)
        .find("." + HtmlClassDef.DECLARED_BID_N)
        .replaceWith(li(cls := HtmlClassDef.DECLARED_BID_N, bid).render)
        .show()
    }
  }

  def nextPlay(playerId: PlayerId): Unit = {
    playerToElement
      .get(playerId)
      .foreach(playerDiv => {
        jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_THINKING).show()
      })
  }

  def played(playerId: PlayerId): Unit = {
    for {
      nOfHands <- playerToNOfHands.get(playerId)
      playerDiv <- playerToElement.get(playerId)
      newNOfHands = nOfHands - 1
    } {
      playerToNOfHands.put(playerId, newNOfHands)
      jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_THINKING).hide()
      jquery(playerDiv)
        .find("." + HtmlClassDef.PLAYER_N_OF_HANDS)
        .replaceWith(li(cls := HtmlClassDef.PLAYER_N_OF_HANDS, newNOfHands).render)
    }
  }

  def trickFinished(trickWinnerId: PlayerId): Unit = {
    for {
      nOfTakenTrick <- playerToNOfTakenTrick.get(trickWinnerId)
      playerDiv <- playerToElement.get(trickWinnerId)
      newNOfTakenTrick = nOfTakenTrick + 1
    } {
      playerToNOfTakenTrick.update(trickWinnerId, newNOfTakenTrick)
      jquery(playerDiv)
        .find("." + HtmlClassDef.PLAYER_TAKEN_TRICK)
        .replaceWith(li(cls := HtmlClassDef.PLAYER_TAKEN_TRICK, newNOfTakenTrick).render)
    }
  }

  def nextTrickLeadPlayerChange(changePlayerClickCallback: (PlayerId, () => Unit) => Unit): Unit = {
    for {
      (playerId, playerDiv) <- playerToElement
    } {
      jquery(playerDiv).bind("click", (_: Element, ev: JQueryEventObject) => {
        changePlayerClickCallback(playerId,
          () => playerToElement.values.map(jquery.apply).foreach(_.unbind(ev)))
      })
    }
  }

  def playerThinking(thinkingPlayerId: PlayerId): Unit = {
    for {
      playerDiv <- playerToElement.get(thinkingPlayerId)
    } {
      jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_THINKING).show()
    }
  }

  def playerThinkingDone(thinkingPlayerId: PlayerId): Unit = {
    for {
      playerDiv <- playerToElement.get(thinkingPlayerId)
    } {
      jquery(playerDiv).find("." + HtmlClassDef.PLAYER_STATUS_THINKING).hide()
    }
  }

}
