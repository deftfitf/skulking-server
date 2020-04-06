package jp.skulking.protocol

sealed trait ClientProtocol {
  val event: String
}

object ClientProtocol {

  case class Connect(override val event: String = "connect", sessionId: String) extends ClientProtocol

  case class Disconnect(override val event: String = "disconnect") extends ClientProtocol

  case class CreateRoom(override val event: String = "createRoom", nOfRound: Int, deckId: String, roomMaxSize: Int) extends ClientProtocol

  case class Join(override val event: String = "join", roomId: String) extends ClientProtocol

  case class Leave(override val event: String = "leave") extends ClientProtocol

  case class Start(override val event: String = "start") extends ClientProtocol

  case class BidDeclare(override val event: String = "bidDeclare", number: Int) extends ClientProtocol

  case class PlayTrick(override val event: String = "playTrick", cardId: String) extends ClientProtocol

  case class PlayTigress(override val event: String = "playTigress", cardId: String, isPirates: Boolean) extends ClientProtocol

  case class PlayRascalOfRoatan(override val event: String = "playRascalOfRoatan", cardId: String, betScore: Int) extends ClientProtocol

  case class ChangeDealer(override val event: String = "changeDealer", newDealerId: String) extends ClientProtocol

  case class ChangeHand(override val event: String = "changeHand", returnCards: List[String]) extends ClientProtocol

  case class FuturePredicateFinish(override val event: String = "futurePredicateFinish") extends ClientProtocol

  case class BidDeclareChange(override val event: String = "bidDeclareChange", changedBid: Int) extends ClientProtocol

  case class Replay(override val event: String = "replay") extends ClientProtocol

  case class Finish(override val event: String = "finish") extends ClientProtocol

}