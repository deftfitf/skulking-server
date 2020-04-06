package jp.skulking.domain

import jp.skulking.domain.Card.{Pirates, PiratesEvent}
import jp.skulking.domain.Round.{TrickFinished, TrickResult}
import jp.skulking.domain.Skulking.BidDeclare

import scala.collection.immutable.Queue

case class Round(
                  currentRound: Int,
                  private val currentTrick: Int,
                  dealerId: PlayerId,
                  field: Field, players: Queue[Player],
                  deck: List[Card], stack: List[CardId]) {

  def isLastTrick: Boolean = currentTrick == currentRound

  def isRoundFinished: Boolean = currentTrick > currentRound

  def lastWinnerId: PlayerId = dealerId

  def lastPlayerId: PlayerId = players.last.id

  def nextPlayerId: PlayerId = players.head.id

  /**
   * @param playerId player_id for bonus
   * @param bonus    Bonus to grant
   * @return Returns a Round with bonus applied to player_id
   */
  def applyBonus(playerId: PlayerId, bonus: Int): Round =
    copy(
      players = players.map(
        player => if (player.id == playerId) player.getBonus(bonus) else player))

  def applyWinTrick(winnerId: PlayerId): Round =
    copy(
      players = players.map(
        player => if (player.id == winnerId) player.winTrick() else player))

  def declaredBidOf(playerId: PlayerId): Int =
    players.find(_.id == playerId).get.declareBid

  /**
   * @param playerId player_id for changing
   * @param delta    Delta to apply
   * @return Returns a Round with the delta bid change applied to player_id
   */
  def changeBid(playerId: PlayerId, delta: Int): Round =
    copy(
      players = players.map(
        player => if (player.id == playerId) player.changeBid(currentRound, delta) else player))

  /**
   * @param dealerId new dealer
   * @return Returns the Round that rotated the player with dealer_id as the new dealer
   */
  def roundPlayers(dealerId: PlayerId): Round =
    copy(players = Round.roundPlayers(dealerId, players))

  /**
   * @param playerId Players who draw cards
   * @param n        number of draws
   * @return Draw cards from n decks.
   *         Returns the Round after drawing and a tuple of the drawn card list
   */
  def drew(playerId: PlayerId, n: Int): (Round, List[CardId]) = {
    val drawCards = deck.take(n)
    (copy(
      deck = deck.drop(n),
      players = players.map(
        player => if (player.id == playerId) player.drawCards(drawCards) else player)),
      drawCards.map(_.id))
  }

  /**
   * @return Calculate the score for the current round
   */
  def calcScore(): RoundScore =
    RoundScore(players.toList map (player => (player.id, player.roundScore(currentRound))))

  /**
   * Play with the card owned by player_id.
   * Returns one of the Round.PlayResult holding the new Round held result.
   *
   * @param playerId player_id to play
   * @param card     card to play
   * @return [[Round.PlayResult]]
   */
  def play(playerId: PlayerId, card: Card): Round.PlayResult =
    if (players.head.id == playerId) {
      if (field.canPut(card) || players.head.cards.forall(card => !field.canPut(card))) {
        players.head play card match {
          case Some(played) => nextPlay(played, card).playFinished()
          case None => Round.InvalidInput
        }
      } else {
        Round.InvalidInput
      }
    } else Round.InvalidInput

  private def playFinished(): Round.PlayResult =
    if (players.head.id == dealerId) trickFinished()
    else Round.PlaySuccess(this)

  private def trickFinished(): TrickFinished =
    trickResult() match {
      case TrickResult.APlayerWon(winnerId, pirates: Pirates, trickBonus) =>
        val (finished, event) = pirates.effect(winnerId,
          applyBonus(winnerId, trickBonus + field.capturingBonusPoints)
            .applyWinTrick(winnerId)
            .nextTrick(winnerId))
        TrickFinished(finished, event)
      case TrickResult.APlayerWon(winnerId, _, trickBonus) =>
        TrickFinished(
          applyBonus(winnerId, trickBonus + field.capturingBonusPoints)
            .applyWinTrick(winnerId)
            .nextTrick(winnerId), PiratesEvent.NoEvent)
      case TrickResult.AllRanAway =>
        TrickFinished(nextTrick(dealerId), PiratesEvent.NoEvent)
      case TrickResult.KrakenAppeared(mustHaveWon) =>
        TrickFinished(nextTrick(mustHaveWon), PiratesEvent.NoEvent)
    }

  private def nextPlay(played: Player, playedCard: Card): Round =
    Round(
      currentRound = currentRound,
      currentTrick = currentTrick,
      dealerId = dealerId,
      field = field.push(played.id, playedCard),
      players = players.tail.enqueue(played),
      deck = deck,
      stack = playedCard.id :: stack)

  private def nextTrick(winnerId: PlayerId): Round = {
    copy(
      currentTrick = currentTrick + 1,
      field = Field.empty,
      stack = field.cardIds ::: stack,
      dealerId = winnerId,
      players = Round.roundPlayers(winnerId, players))
  }

  /**
   * @return Some: when there is a winner
   *         Returns player_id, winning card, and bonus points earned
   *         None: when there is not a winner
   */
  private def trickResult(): TrickResult = {
    val (winner, winCard) = field.battle
    winCard match {
      case _: Card.Escape => TrickResult.AllRanAway
      case Card.Tigress(_, isPirates) if !isPirates => TrickResult.AllRanAway
      case _: Card.Skulking =>
        field.firstMermaid match {
          case Some(mermaidPlayer) => TrickResult.APlayerWon(mermaidPlayer, winCard, 50)
          case None => TrickResult.APlayerWon(winner, winCard, 30 * field.nOfPirates)
        }
      case _: Card.Kraken =>
        val (mustHaveWon, _) = field.remove(winner).battle
        TrickResult.KrakenAppeared(mustHaveWon)
      case _ => TrickResult.APlayerWon(winner, winCard, 0)
    }
  }

  def updateWithBidDeclares(bidDeclares: Map[PlayerId, BidDeclare]): Round =
    copy(players = players.map(
      player => player.changeBid(currentRound, bidDeclares(player.id).declareBid)))

  def currentPlayerHands: Map[PlayerId, Set[CardId]] =
    players.map(player => (player.id, player.cards.map(_.id))).toMap

}

object Round {

  sealed trait PlayResult

  case object InvalidInput extends PlayResult

  case class PlaySuccess(played: Round) extends PlayResult

  case class TrickFinished(trickFinished: Round, piratesEvent: PiratesEvent) extends PlayResult

  sealed trait TrickResult

  object TrickResult {

    case class APlayerWon(playerId: PlayerId, card: Card, trickBonus: Int) extends TrickResult

    case object AllRanAway extends TrickResult

    case class KrakenAppeared(mustHaveWon: PlayerId) extends TrickResult

  }

  case class RoundConfiguration(maxRound: Int)

  private def dealCards(round: Int, playerIds: List[PlayerId], deck: List[Card]): (List[Player], List[Card]) = {
    @scala.annotation.tailrec
    def recursive(playerIds: List[PlayerId], deck: List[Card], players: List[Player]): (List[Player], List[Card]) =
      playerIds match {
        case Nil => (players, deck)
        case playerId :: tail =>
          recursive(tail, deck.drop(round),
            Player.of(playerId, -1, deck.take(round).toSet) :: players)
      }

    val (players, dealDeck) = recursive(playerIds, deck, Nil)
    (players.reverse, dealDeck)
  }

  private def roundPlayers(dealerId: PlayerId, players: Queue[Player]): Queue[Player] = {
    @scala.annotation.tailrec
    def recursive(players: Queue[Player]): Queue[Player] =
      if (players.head.id == dealerId) players
      else recursive(players.tail.enqueue(players.head))

    recursive(players)
  }

  def newRound(dealerId: PlayerId, round: Int, playerIds: List[PlayerId], deck: List[Card]): Round = {
    val (players, dealDeck) = dealCards(round, playerIds, deck)
    Round(
      currentRound = round,
      currentTrick = 1,
      dealerId = dealerId,
      field = Field.empty,
      players = roundPlayers(dealerId, Queue(players: _*)),
      deck = dealDeck,
      stack = Nil)
  }

}