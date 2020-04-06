package jp.skulking.domain

import scala.collection.immutable.Queue

case class Field(field: Queue[(PlayerId, Card)]) {

  def canPut(card: Card): Boolean = {
    @scala.annotation.tailrec
    def recursive(field: Queue[Card]): Boolean =
      if (field.nonEmpty) {
        field.head.canFollow(card) match {
          case Some(mustFollow) => mustFollow
          case None => recursive(field.tail)
        }
      } else true

    recursive(field.map(_._2))
  }

  def push(playerId: PlayerId, card: Card): Field =
    copy(field = field.enqueue((playerId, card)))

  def cardIds: List[CardId] =
    field.map(_._2.id).toList

  def battle: (PlayerId, Card) =
    field reduce[(PlayerId, Card)] { case ((pId1, pCard1), (pId2, pCard2)) =>
      if ((pCard1 battle pCard2) == pCard1) (pId1, pCard1)
      else (pId2, pCard2)
    }

  def remove(playerId: PlayerId): Field =
    copy(field = field.filterNot(_._1 == playerId))

  def capturingBonusPoints: Int =
    field.map(_._2.capturingBonusPoint).sum

  def firstMermaid: Option[PlayerId] =
    field.find { case (_, card) =>
      card match {
        case _: Card.Mermaid => true
        case _ => false
      }
    }.map(_._1)

  def nOfPirates: Int =
    field.map(_._2).count {
      case _: Card.Pirates => true
      case _ => false
    }

}

object Field {

  val empty: Field = Field(Queue.empty)

}
