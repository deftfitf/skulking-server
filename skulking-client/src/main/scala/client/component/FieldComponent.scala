package client.component

import client.jquery
import jp.skulking.domain.Card.Tigress
import jp.skulking.domain.Skulking.Deck
import jp.skulking.domain.{Card, CardId, Field, PlayerId}
import org.scalajs.dom.Element
import org.scalajs.dom.html.Div
import org.scalajs.jquery.JQueryEventObject
import scalatags.JsDom.all._

final case class FieldComponent(parentId: String, deck: Deck) {
  private val fieldId = s"$parentId-field"
  val self: Div = div(
    id := fieldId
  ).render

  private[this] var field: Field = Field.empty

  def getField: Field = field

  def bidCardDivFormat(bid: Int): Div =
    div(
      cls := s"card field-bid-card-$bid"
    ).render

  def cardDivFormat(cardId: CardId): Div =
    div(
      cls := s"card field-card-${cardId.value}"
    ).render

  def tigressCardDivFormat(cardId: CardId, isPirates: Boolean): Div =
    div(
      cls := s"card field-card-${cardId.value}-${if (isPirates) "pirates" else "runaway"}"
    ).render

  def showBidCards(round: Int, cardClickCallback: Int => Unit): Unit = {
    for {
      bid <- 0 to round
      bidCard = bidCardDivFormat(bid)
    } {
      jquery(bidCard).bind("click", (_: Element, _: JQueryEventObject) => {
        cardClickCallback(bid)
      })
      jquery(self).append(bidCard)
    }
  }

  def showDeckCards(deck: List[CardId]): Unit = {
    for {
      cardId <- deck
      cardDiv = cardDivFormat(cardId)
    } {
      jquery(self).append(cardDiv)
    }
  }

  def removeExcept(bid: Int): Unit = {
    jquery(self).children().not(bidCardDivFormat(bid)).remove()
  }

  def played(playerId: PlayerId, cardId: CardId): Unit = {
    for {
      card <- deck.of[Card](cardId)
      cardDiv = cardDivFormat(cardId)
    } {
      field = field.push(playerId, card)
      jquery(self).append(cardDiv)
    }
  }

  def tigressPlayed(playerId: PlayerId, cardId: CardId, isPirates: Boolean): Unit = {
    for {
      card <- deck.of[Card](cardId).flatMap {
        case t: Tigress => Some(t.pirates(isPirates))
        case _ => None
      }
      tigressCardDiv = tigressCardDivFormat(cardId, isPirates)
    } {
      field = field.push(playerId, card)
      jquery(self).append(tigressCardDiv)
    }
  }

  def clear(): Unit = {
    field = Field.empty
    jquery(self).empty()
  }

}
