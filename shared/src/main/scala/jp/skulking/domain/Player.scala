package jp.skulking.domain

case class Player(id: PlayerId, declareBid: Int, takeTrick: Int, cards: Set[Card], takenBonus: Int) {

  def play(card: Card): Option[Player] =
    if (cards.contains(card)) Some(copy(cards = cards - card))
    else None

  def winTrick(): Player =
    copy(takeTrick = takeTrick + 1)

  def getBonus(bonus: Int): Player =
    copy(takenBonus = takenBonus + bonus)

  def drawCards(cards: List[Card]): Player =
    copy(cards = this.cards ++ cards)

  def changeBid(currentRound: Int, newBid: Int): Player = {
    if (newBid <= currentRound && newBid >= 0)
      copy(declareBid = newBid)
    else this
  }

  def roundScore(numberOfTricks: Int): (Int, Int) =
    if (declareBid == takeTrick) {
      if (declareBid == 0) (numberOfTricks * 10, takenBonus)
      else (declareBid * 20, takenBonus)
    } else {
      if (declareBid == 0) (-(numberOfTricks * 10), 0)
      else (-((declareBid - takeTrick).abs * 10), 0)
    }

}

object Player {

  def of(playerId: PlayerId, declareBid: Int, cards: Set[Card]): Player =
    Player(playerId, declareBid, 0, cards, 0)

}
