package jp.skulking.protocol

sealed trait ServerProtocol {
  val event: String
}

object ServerProtocol {

  case class CreateRoomSucceeded(override val event: String = "createRoomSucceeded", playerId: String, roomId: String) extends ServerProtocol

  case class JoinSucceeded(override val event: String = "joinSucceeded",
                           playerId: String,
                           masterId: String,
                           nOfRound: Int,
                           deckId: String,
                           roomMaxSize: Int,
                           roomMinSize: Int,
                           playerIds: List[String]) extends ServerProtocol

  case class Connected(override val event: String = "connected", playerId: String) extends ServerProtocol

  case class Disconnect(override val event: String = "disconnect", reason: String) extends ServerProtocol

  case class Disconnected(override val event: String = "disconnected", reason: String) extends ServerProtocol

  case class NewPlayerJoined(override val event: String = "newPlayerJoined", playerId: String) extends ServerProtocol

  case class MasterLeave(override val event: String = "masterLeave", leaveMater: String, nextMaster: String) extends ServerProtocol

  case class PlayerLeave(override val event: String = "playerLeave", playerId: String) extends ServerProtocol

  case class GameStarted(override val event: String = "gameStarted") extends ServerProtocol

  case class NewRoundStarted(override val event: String = "newRoundStarted", round: Int, hands: List[String], restOfDeck: Int) extends ServerProtocol

  case class BidDeclared(override val event: String = "bidDeclared", playerId: String) extends ServerProtocol

  case class TrickPhaseStarted(override val event: String = "trickPhaseStarted", bidDeclares: List[(String, Int)]) extends ServerProtocol

  case class Played(override val event: String = "played", playerId: String, cardId: String) extends ServerProtocol

  case class TigressPlayed(override val event: String = "tigressPlayed", playerId: String, cardId: String, isPirates: Boolean) extends ServerProtocol

  case class RascalOfRoatanPlayed(override val event: String = "rascalOfRoatanPlayed", playerId: String, cardId: String, betScore: Int) extends ServerProtocol

  case class TrickFinished(override val event: String = "trickFinished", trickWinner: String) extends ServerProtocol

  case class NextTrickLeadPlayerChangeableNotice(override val event: String = "nextTrickLeadPlayerChangeableNotice") extends ServerProtocol

  case class NextTrickLeadPlayerChangingNotice(override val event: String = "nextTrickLeadPlayerChangingNotice", changingplayerId: String) extends ServerProtocol

  case class HandChangeAvailableNotice(override val event: String = "handChangeAvailableNotice", drawCards: List[String]) extends ServerProtocol

  case class HandChangingNotice(override val event: String = "handChangingNotice", changingplayerId: String) extends ServerProtocol

  case class GotBonusScore(override val event: String = "gotBonusScore", playerId: String, bonusScore: Int) extends ServerProtocol

  case class FuturePredicated(override val event: String = "futurePredicated", deckCard: List[String]) extends ServerProtocol

  case class FuturePredicatedOtherPlayer(override val event: String = "futurePredicatedOtherPlayer", playerId: String) extends ServerProtocol

  case class FuturePredicateFinished(override val event: String = "futurePredicateFunished", playerId: String) extends ServerProtocol

  case class DeclareBidChangeAvailableNotice(override val event: String = "declareBidChangeAvailableNotice", playerId: String, min: Int, max: Int) extends ServerProtocol

  case class DeclareBidChangingNotice(override val event: String = "declareBidChangingNotice", playerId: String) extends ServerProtocol

  case class DeclareBidChanged(override val event: String = "declareBidChanged", playerId: String, changedBid: Int) extends ServerProtocol

  case class NextPlay(override val event: String = "nextPlay", nextPlayerId: String) extends ServerProtocol

  case class RoundFinished(override val event: String = "roundFinished", round: Int, roundScore: List[(String, (Int, Int))]) extends ServerProtocol

  case class GameFinished(override val event: String = "gameFinished", gameWinner: String, gameScore: Map[String, Int]) extends ServerProtocol

  case class NextTrickChanged(override val event: String = "nextTrickChanged", from: String, to: String) extends ServerProtocol

  case class HandChanged(override val event: String = "handChanged", changed: String, nOfChanged: Int) extends ServerProtocol

  case class ReplayGame(override val event: String = "replayGame") extends ServerProtocol

  case class ShutdownGame(override val event: String = "shutdownGame") extends ServerProtocol

}