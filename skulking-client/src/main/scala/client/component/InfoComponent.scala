package client.component

import client.jquery
import jp.skulking.domain.Skulking.Deck
import org.scalajs.dom.html.Div
import scalatags.JsDom.all._

final case class InfoComponent(parentId: String,
                               nOfRound: Int,
                               deck: Deck,
                               roomMaxSize: Int,
                               roomMinSize: Int) {
  private val infoDivId = s"$parentId-roundinfo-info"

  val self: Div = div(id := infoDivId).render

  private def infoDivFormat(round: Int): Div =
    div(
      id := infoDivId,
      ul(
        li(s"${nOfRound}ラウンド制"),
        li(deck.id.value),
        li(s"部屋人数 最大${roomMaxSize} 最低${roomMinSize} "),
        li(if (round > 0) s"現在${round}ラウンド目"
        else "ゲーム開始までしばらくお待ちください")
      )
    ).render

  def updateRound(round: Int): Unit = {
    jquery("#" + infoDivId).replaceWith(infoDivFormat(round))
  }

}
