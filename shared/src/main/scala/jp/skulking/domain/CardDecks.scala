package jp.skulking.domain

import jp.skulking.domain.Card._
import jp.skulking.domain.Skulking.Deck

import scala.util.Random

object CardDecks {

  implicit val rnd: Random = new Random()

  val colorCards: Seq[Card] =
    for {
      color <- CardColor.colorList
      n <- 1 to 14
    } yield NumberCard(CardId(color.toString + n), n, color)

  val escapeCards: Seq[Card] =
    (for {
      n <- 1 to 5
    } yield Escape(CardId(s"Escape-$n escape-card")))

  val nonAbilityPirates: Seq[Card] =
    (for {
      n <- 1 to 5
    } yield NonAbilityPirates(CardId(s"Pirates-$n pirates-card")))

  val standardDeck =
    Deck(
      DeckId("standard"),
      (colorCards ++
        escapeCards ++
        nonAbilityPirates ++
        Seq[Card](
          Tigress(CardId("Tigress")),
          Card.Skulking(CardId("Skulking"))
        )).toList)

  val expansionDeck =
    Deck(
      DeckId("expansion"),
      (colorCards ++
        escapeCards ++
        (for {
          n <- 1 to 2
        } yield Mermaid(CardId(s"Mermaid-$n"))) ++
        Seq[Card](
          RoiseDLaney(CardId("RoiseDLaney")),
          BahijTheBandit(CardId("BahijTheBandit")),
          RascalOfRoatan(CardId("RascalOfRoatan")),
          JuanitaJade(CardId("JuanitaJade")),
          HarryTheGiant(CardId("HarryTheGiant")),
          Tigress(CardId("Tigress")),
          Card.Skulking(CardId("Skulking")),
          Kraken(CardId("Kraken"))
        )).toList)

  val decks: Map[DeckId, Deck] =
    Map(
      (DeckId("standard"), standardDeck),
      (DeckId("expansion"), expansionDeck))

}