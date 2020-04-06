package jp.skulking.domain

sealed trait Card {
  val id: CardId
  protected val mustFollow: MustFollow

  /**
   * If get this card by trick, you can get this point
   */
  val capturingBonusPoint = 0

  /**
   * Left join function to battle between cards
   *
   * @return winner card
   */
  def battle(card: Card): Card

  /**
   * Checks whether the must follow of the argument card is satisfied
   *
   * @param card Leading card
   * @return Some(true) when meets the requirements of MustFollow
   *         Some(false) when mustFollow condition is not met
   *         None when must follow is not defined. Anything can follow
   */
  def canFollow(card: Card): Option[Boolean] =
    mustFollow.canFollow(card.mustFollow)

  override def equals(obj: Any): Boolean =
    obj match {
      case card: Card => id == card.id
      case _ => false
    }

}

object Card {

  sealed trait CardColor

  object CardColor {
    val colorList: Seq[CardColor] = Seq(Green, Yellow, Purple, Black)

    case object Green extends CardColor

    case object Yellow extends CardColor

    case object Purple extends CardColor

    case object Black extends CardColor

  }

  case class NumberCard(override val id: CardId, number: Int, cardColor: CardColor) extends Card {
    override protected val mustFollow: MustFollow = MustFollow.ColorCard(cardColor)

    override def battle(card: Card): Card =
      card match {
        case NumberCard(_, n, color) if color == cardColor =>
          if (number > n) this else card
        case NumberCard(_, _, CardColor.Black) => card
        case NumberCard(_, _, _) => this
        case _: Pirates | Tigress(_, true) | _: Skulking | _: Mermaid | _: Kraken => card
        case _: Escape | Tigress(_, false) => this
      }

    override val capturingBonusPoint: Int =
      if (number == 14)
        cardColor match {
          case CardColor.Black => 20
          case _ => 10
        }
      else 0
  }

  sealed trait PiratesEvent

  object PiratesEvent {

    case object NoEvent extends PiratesEvent

    case class NextTrickLeadPlayerChangeableNotice(playerId: PlayerId) extends PiratesEvent

    case class HandChangeAvailableNotice(playerId: PlayerId, drawCard: List[CardId]) extends PiratesEvent

    case class GotBonusScore(playerId: PlayerId, bonusScore: Int) extends PiratesEvent

    case class FuturePredicated(playerId: PlayerId, deckCard: List[CardId]) extends PiratesEvent

    case class DeclareBidChangeAvailable(playerId: PlayerId, min: Int, max: Int) extends PiratesEvent

  }

  sealed trait Pirates extends Card {
    override protected val mustFollow: MustFollow = MustFollow.AnyCard

    override def battle(card: Card): Card =
      Pirates.battle(this, card)

    val abilityOnlyUseLastTrick: Boolean = false

    def effect(winnerId: PlayerId, round: Round): (Round, PiratesEvent)
  }

  object Pirates {
    def battle(left: Card, right: Card): Card =
      right match {
        case _: Skulking | _: Kraken => right
        case _ => left
      }
  }

  case class NonAbilityPirates(override val id: CardId) extends Pirates {
    override def effect(winnerId: PlayerId, round: Round): (Round, PiratesEvent) =
      (round, PiratesEvent.NoEvent)
  }

  case class RoiseDLaney(override val id: CardId) extends Pirates {
    override def effect(winnerId: PlayerId, round: Round): (Round, PiratesEvent) =
      if (!round.isLastTrick) {
        (round, PiratesEvent.NextTrickLeadPlayerChangeableNotice(winnerId))
      } else {
        (round, PiratesEvent.NoEvent)
      }
  }

  case class BahijTheBandit(override val id: CardId) extends Pirates {
    override def effect(winnerId: PlayerId, round: Round): (Round, PiratesEvent) =
      if (!round.isLastTrick) {
        val (r, c) = round.drew(winnerId, 2)
        (r, PiratesEvent.HandChangeAvailableNotice(winnerId, c))
      } else {
        (round, PiratesEvent.NoEvent)
      }
  }

  case class RascalOfRoatan(override val id: CardId, private val betScore: Int = 0) extends Pirates {

    def bet(betScore: Int): Option[RascalOfRoatan] = {
      betScore match {
        case 10 => Some(copy(betScore = 10))
        case 20 => Some(copy(betScore = 20))
        case _ => None
      }
    }

    override val abilityOnlyUseLastTrick: Boolean = true

    override def effect(winnerId: PlayerId, round: Round): (Round, PiratesEvent) =
      if (round.isLastTrick && betScore > 0) {
        (round.applyBonus(round.lastWinnerId, betScore),
          PiratesEvent.GotBonusScore(winnerId, betScore))
      } else {
        (round, PiratesEvent.NoEvent)
      }
  }

  case class JuanitaJade(override val id: CardId) extends Pirates {
    override def effect(winnerId: PlayerId, round: Round): (Round, PiratesEvent) =
      if (!round.isLastTrick) {
        (round, PiratesEvent.FuturePredicated(winnerId, round.deck.map(_.id)))
      } else {
        (round, PiratesEvent.NoEvent)
      }

  }

  case class HarryTheGiant(override val id: CardId) extends Pirates {

    override val abilityOnlyUseLastTrick: Boolean = true

    override def effect(winnerId: PlayerId, round: Round): (Round, PiratesEvent) =
      if (round.isLastTrick) {
        val currentBid = round.declaredBidOf(winnerId)
        (round,
          PiratesEvent.DeclareBidChangeAvailable(winnerId,
            0 min (currentBid - 1),
            (currentBid + 1) max round.currentRound))
      } else {
        (round, PiratesEvent.NoEvent)
      }
  }

  case class Tigress(override val id: CardId, isPirates: Boolean = false) extends Card {
    override protected val mustFollow: MustFollow =
      if (isPirates) MustFollow.AnyCard
      else MustFollow.Undefined

    override def battle(card: Card): Card =
      if (isPirates) Pirates.battle(this, card)
      else Escape.battle(this, card)

    def pirates(isPirates: Boolean): Tigress = copy(isPirates = isPirates)
  }

  case class Escape(override val id: CardId) extends Card {
    override protected val mustFollow: MustFollow = MustFollow.Undefined

    override def battle(card: Card): Card = Escape.battle(this, card)
  }

  object Escape {
    def battle(left: Card, right: Card): Card = right
  }

  case class Skulking(override val id: CardId) extends Card {
    override protected val mustFollow: MustFollow = MustFollow.AnyCard

    override def battle(card: Card): Card =
      card match {
        case _: Mermaid | _: Kraken => card
        case _ => this
      }
  }

  case class Mermaid(override val id: CardId) extends Card {
    override protected val mustFollow: MustFollow = MustFollow.AnyCard

    override def battle(card: Card): Card =
      card match {
        case _: Pirates | _: Skulking | _: Kraken => card
        case _ => this
      }
  }

  case class Kraken(override val id: CardId) extends Card {
    override protected val mustFollow: MustFollow = MustFollow.Undefined

    override def battle(card: Card): Card = this
  }

}