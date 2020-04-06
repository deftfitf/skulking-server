package jp.skulking.domain

import jp.skulking.domain.Card.PiratesEvent
import jp.skulking.domain.Skulking.Deck

import scala.util.Random

sealed trait Skulking {
  val nOfRounds: Int
  val deck: Deck
  val roundScores: List[RoundScore]
  val playerIds: List[PlayerId]

  def lastRoundScore: RoundScore =
    roundScores.head

}

object Skulking {

  case class BidDeclare(playerId: PlayerId, declareBid: Int)

  case class Deck(id: DeckId, cards: List[Card])(implicit rnd: Random) {
    private val index: Map[CardId, Card] = cards.map(card => (card.id, card)).toMap

    def of[T <: Card](cardId: CardId): Option[T] =
      (index get cardId) match {
        case Some(card: T) => Some(card)
        case _ => None
      }

    def shuffled(): List[Card] = rnd.shuffle(cards)
  }

  sealed trait TrickFinishedPhase

  case class BiddingPhase(
                           nOfRounds: Int,
                           playerIds: List[PlayerId],
                           round: Round,
                           deck: Deck,
                           roundScores: List[RoundScore])
    extends Skulking {

    def bid(bidDeclares: Map[PlayerId, BidDeclare]): TrickPhase =
      TrickPhase(
        nOfRounds = nOfRounds,
        playerIds = playerIds,
        round = round.updateWithBidDeclares(bidDeclares),
        deck = deck,
        roundScores = roundScores)

  }

  sealed trait PlayResult

  object PlayResult {

    case object InvalidInput extends PlayResult

    case class PlaySuccess(trickPhase: TrickPhase) extends PlayResult

    case class TrickFinished(trickPhase: TrickFinishedPhase, trickWinner: PlayerId, piratesEvent: PiratesEvent) extends PlayResult

  }

  case class TrickPhase(
                         nOfRounds: Int,
                         playerIds: List[PlayerId],
                         round: Round,
                         deck: Deck,
                         roundScores: List[RoundScore])
    extends Skulking with TrickFinishedPhase {

    def isLastRound: Boolean = round.currentRound == nOfRounds

    def changeDealer(newDealerId: PlayerId): TrickPhase =
      copy(round = round.roundPlayers(newDealerId))

    def changeHand(playerId: PlayerId, returnCards: List[CardId]): TrickPhase =
      copy(round = round.copy(stack = returnCards ::: round.stack))

    def changeBid(playerId: PlayerId, changeBid: Int): TrickPhase =
      copy(round = round.changeBid(playerId, changeBid))

    def play(playerId: PlayerId, card: Card): Skulking.PlayResult =
      (round play(playerId, card)) match {
        case Round.TrickFinished(trickFinished, piratesEvent) =>
          finishTrick(trickFinished, piratesEvent)
        case Round.PlaySuccess(played) =>
          PlayResult.PlaySuccess(copy(round = played))
        case Round.InvalidInput => PlayResult.InvalidInput
      }

    private def finishTrick(finished: Round, piratesEvent: PiratesEvent): Skulking.PlayResult = {
      val trickPhase = copy(round = finished)
      val nextPhase = piratesEvent match {
        case PiratesEvent.NextTrickLeadPlayerChangeableNotice(playerId) =>
          NextTrickLeadPlayerChanging(playerId, trickPhase)
        case PiratesEvent.HandChangeAvailableNotice(playerId, drawCard) =>
          HandChangeWaiting(playerId, drawCard, trickPhase)
        case PiratesEvent.FuturePredicated(playerId, _) =>
          FuturePredicateWaiting(playerId, trickPhase)
        case PiratesEvent.DeclareBidChangeAvailable(playerId, _, _) =>
          BidDeclareChangeWaiting(playerId, trickPhase)
        case _ => trickPhase
      }
      PlayResult.TrickFinished(nextPhase, finished.lastWinnerId, piratesEvent)
    }

    def finished(): Finished =
      Finished(
        lastWinnerId = round.dealerId,
        playerIds = playerIds,
        nOfRounds = nOfRounds,
        deck = deck,
        roundScores = round.calcScore() :: roundScores)

    def roundFinished(): BiddingPhase =
      BiddingPhase(
        nOfRounds = nOfRounds,
        playerIds = playerIds,
        round = Round.newRound(
          dealerId = round.dealerId,
          round = round.currentRound + 1,
          playerIds = playerIds,
          deck.shuffled()),
        deck = deck,
        roundScores = round.calcScore() :: roundScores)

  }

  case class NextTrickLeadPlayerChanging(
                                          changingPlayerId: PlayerId,
                                          trickPhase: TrickPhase)
    extends Skulking with TrickFinishedPhase {
    override val playerIds: List[PlayerId] = trickPhase.playerIds
    override val nOfRounds: Int = trickPhase.nOfRounds
    override val deck: Deck = trickPhase.deck
    override val roundScores: List[RoundScore] = trickPhase.roundScores

    def changeLeadPlayer(newLeadPlayerId: PlayerId): TrickPhase =
      trickPhase.changeDealer(newLeadPlayerId)

  }

  case class HandChangeWaiting(
                                changingPlayerId: PlayerId,
                                drawCard: List[CardId],
                                trickPhase: TrickPhase)
    extends Skulking with TrickFinishedPhase {
    override val playerIds: List[PlayerId] = trickPhase.playerIds
    override val nOfRounds: Int = trickPhase.nOfRounds
    override val deck: Deck = trickPhase.deck
    override val roundScores: List[RoundScore] = trickPhase.roundScores

    def changeHand(returnCards: List[CardId]): TrickPhase =
      trickPhase.changeHand(changingPlayerId, returnCards)

  }

  case class FuturePredicateWaiting(
                                     predicatingPlayerId: PlayerId,
                                     trickPhase: TrickPhase)
    extends Skulking with TrickFinishedPhase {
    override val playerIds: List[PlayerId] = trickPhase.playerIds
    override val nOfRounds: Int = trickPhase.nOfRounds
    override val deck: Deck = trickPhase.deck
    override val roundScores: List[RoundScore] = trickPhase.roundScores

    def finish(): TrickPhase =
      trickPhase

  }

  case class BidDeclareChangeWaiting(
                                      changingPlayerId: PlayerId,
                                      trickPhase: TrickPhase)
    extends Skulking with TrickFinishedPhase {
    override val playerIds: List[PlayerId] = trickPhase.playerIds
    override val nOfRounds: Int = trickPhase.nOfRounds
    override val deck: Deck = trickPhase.deck
    override val roundScores: List[RoundScore] = trickPhase.roundScores

    def change(playerId: PlayerId, changedBid: Int): TrickPhase =
      trickPhase.changeBid(playerId, changedBid)

  }

  case class Finished(lastWinnerId: PlayerId, playerIds: List[PlayerId], nOfRounds: Int, deck: Deck, roundScores: List[RoundScore]) extends Skulking {

    def replay(): BiddingPhase =
      newGame(lastWinnerId, playerIds: List[PlayerId], nOfRounds, deck)

    def gameWinnerId: PlayerId =
      aggregate.maxBy(_._2)._1

    lazy val aggregate: Map[PlayerId, Int] =
      roundScores
        .flatMap(_.scores)
        .groupBy(_._1)
        .map(entry => (entry._1, entry._2.map(score => score._2._1 + score._2._2).sum))

  }

  def newGame(dealerId: PlayerId, playerIds: List[PlayerId], nOfRounds: Int, deck: Deck): BiddingPhase =
    BiddingPhase(
      nOfRounds = nOfRounds,
      playerIds = playerIds,
      round = Round.newRound(
        dealerId = dealerId,
        round = 1,
        playerIds = playerIds,
        deck.shuffled()),
      deck = deck,
      roundScores = Nil)

}