package client.component

import client.{HtmlClassDef, jquery}
import jp.skulking.domain.Card.Tigress
import jp.skulking.domain.Skulking.Deck
import jp.skulking.domain.{Card, CardId, Field}
import org.scalajs.dom.Element
import org.scalajs.dom.html.Div
import org.scalajs.jquery.JQueryEventObject
import scalatags.JsDom.all._

final case class HandsComponent(parentId: String,
                                deck: Deck) {
  private val handsDivId = s"$parentId-roundinfo-hands"
  private val deckDivId = s"$parentId-roundinfo-deck"

  val self: Div =
    div(
      id := s"$parentId-roundinfo",
      div(id := handsDivId),
      deckDivFormat(deck.cards.size)).render

  import scala.collection.mutable

  private[this] val handToDiv = mutable.Map.empty[CardId, Div]

  def drawCards(hands: List[CardId], restOfDeck: Int): Unit = {
    drawCards(hands)
    jquery("#" + deckDivId).replaceWith(deckDivFormat(restOfDeck))
  }

  def drawCards(hands: List[CardId]): Unit = {
    hands.map(card => div(cls := HtmlClassDef.CARD + " " + card.value).render)
      .zip(hands)
      .foreach(zipped => {
        handToDiv.put(zipped._2, zipped._1)
        jquery("#" + handsDivId).append(zipped._1)
      })
  }

  private def deckDivFormat(restOfDeck: Int): Div =
    div(
      id := deckDivId,
      div(cls := HtmlClassDef.CARD + " " + HtmlClassDef.DECK_DIV),
      div(s"残り${restOfDeck}枚")
    ).render

  def playMyTurn(field: Field, cardClickCallback: (Card, () => Unit) => Unit): Unit = {
    val (canPutCards, notPutCards) = handToDiv.keys
      .flatMap(deck.of[Card])
      .partition {
        case t: Tigress =>
          field.canPut(t.pirates(false)) ||
            field.canPut(t.pirates(true))
        case c => field.canPut(c)
      }

    notPutCards
      .map(_.id)
      .flatMap(handToDiv.get)
      .map(jquery.apply)
      .foreach(_.addClass("un-selectable-card"))

    for {
      canPutCard <-
        if (canPutCards.nonEmpty) canPutCards
        else handToDiv.keys.flatMap(deck.of[Card])
      canPutCardDiv <- handToDiv.get(canPutCard.id)
    } {
      jquery(canPutCardDiv).addClass("selectable-card")
      jquery(canPutCardDiv).bind("click", (_: Element, _: JQueryEventObject) => {
        cardClickCallback(canPutCard,
          () => {
            handToDiv -= canPutCard.id
            jquery(canPutCardDiv).remove()
            handToDiv.values.map(jquery.apply).foreach { d =>
              d.removeClass("un-selectable-card")
              d.removeClass("selectable-card")
              d.unbind("click")
            }
          })
      })
    }
  }

  class SelectableCardsStatus {

    import scala.collection.mutable

    private[this] val selected = mutable.Set.empty[CardId]

    def isSelected(cardId: CardId): Boolean =
      selected.contains(cardId)

    def select(cardId: CardId): Unit = {
      selected += cardId
    }

    def unSelect(cardId: CardId): Unit = {
      selected -= cardId
    }

    def selectedAll: List[CardId] =
      selected.toList

  }

  def selectableCards(
                       nOfSelect: Int,
                       popUpInterfaceComponent: PopUpInterfaceComponent,
                       doneSelect: List[CardId] => Unit): Unit = {
    val status = new SelectableCardsStatus()
    for {
      cardId <- handToDiv.keys
      cardDiv <- handToDiv.get(cardId)
    } {
      jquery(cardDiv).addClass("selectable-card")
      jquery(cardDiv).bind("click", (_: Element, _: JQueryEventObject) => {
        if (!status.isSelected(cardId)) {
          if (status.selectedAll.size < nOfSelect) {
            status.select(cardId)
            jquery(cardDiv).addClass(HtmlClassDef.CHECKED_CARD)
            if (status.selectedAll.size == nOfSelect) {
              popUpInterfaceComponent.yesPopup(
                "このカードの組み合わせを山札に戻しますか?", "はい",
                {
                  status.selectedAll
                    .flatMap(handToDiv.get)
                    .map(jquery.apply)
                    .foreach(_.remove())
                  handToDiv --= status.selectedAll
                  doneSelect(status.selectedAll)
                  handToDiv.values.map(jquery.apply).foreach { d =>
                    d.removeClass("selectable-card")
                    d.unbind("click")
                  }
                }
              )
            }
          }
        } else {
          status.unSelect(cardId)
          jquery(cardDiv).removeClass("selectable-card")
          jquery(cardDiv).removeClass(HtmlClassDef.CHECKED_CARD)
          if (status.selectedAll.size == nOfSelect - 1) {
            popUpInterfaceComponent.clear()
          }
        }
      })
    }
  }


}
