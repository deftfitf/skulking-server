package client.component

import client.{HtmlClassDef, jquery}
import jp.skulking.domain.PlayerId
import org.scalajs.dom.html.{Div, TableSection}
import scalatags.JsDom.all._

final case class ScoreBoardComponent(parentId: String, players: List[PlayerId]) {
  private val selfId = s"$parentId-scoreboard"
  private val tbodyId = s"$parentId-scoreboard-tbody"
  private val tableBody: TableSection = tbody(id := tbodyId).render

  val self: Div =
    div(
      id := selfId,
      table(
        caption("スコアボード"),
        thead(
          tr(
            th("ラウンド"),
            players.map(playerId => th(colspan := 2, s"${playerId.value}"))
          )
        ),
        tableBody
      )
    ).render

  def clear(): Unit =
    jquery(tableBody).children().remove()

  def roundFinished(round: Int, roundScore: Map[PlayerId, (Int, Int)]): Unit = {
    jquery(tableBody).append(
      tr(
        cls := HtmlClassDef.SCORE_BOARD_SCORE_ROW,
        td(rowspan := 2, round),
        players.flatMap(playerId => {
          val (score, bonus) = roundScore(playerId)
          td(score) :: td(bonus) :: Nil
        })
      ).render
    ).append(
      tr(
        cls := HtmlClassDef.SCORE_BOARD_SCORE_SUM_ROW,
        players.map(playerId => {
          val (score, bonus) = roundScore(playerId)
          td(colspan := 2, score + bonus)
        })
      ).render
    )
  }

  def gameFinished(gameScore: Map[PlayerId, Int]): Unit = {
    jquery(tableBody).append(
      tr(
        cls := HtmlClassDef.SCORE_BOARD_SCORE_RESULT_ROW,
        td("total"),
        players.map(playerId => {
          td(colspan := 2, gameScore(playerId))
        })
      ).render
    )
  }

}
