package jp.skulking.domain

import jp.skulking.domain.Card.CardColor

sealed trait MustFollow {

  def canFollow(mustFollow: MustFollow): Option[Boolean]

}

object MustFollow {

  case class ColorCard(cardColor: CardColor) extends MustFollow {
    override def canFollow(mustFollow: MustFollow): Option[Boolean] =
      mustFollow match {
        case ColorCard(c) => Some(c == cardColor)
        case _ => Some(false)
      }
  }

  case object AnyCard extends MustFollow {
    override def canFollow(mustFollow: MustFollow): Option[Boolean] = Some(true)
  }

  case object Undefined extends MustFollow {
    override def canFollow(mustFollow: MustFollow): Option[Boolean] = None
  }

}
