package jp.skulking.protocol

import play.api.libs.json.{Format, JsResult, JsValue, Json}

trait ServerProtocolJSONFormats {

  def deserialize(jsValue: JsValue): ServerProtocol = {
    ServerProtocolJSONFormats.format.reads(jsValue).get
  }

  def serialize(serverProtocol: ServerProtocol): JsValue = {
    ServerProtocolJSONFormats.format.writes(serverProtocol)
  }

}

object ServerProtocolJSONFormats {

  private val defaultConnectedFormat = Json.format[ServerProtocol.Connected]
  private val defaultDisconnectFormat = Json.format[ServerProtocol.Disconnect]
  private val defaultDisconnectedFormat = Json.format[ServerProtocol.Disconnected]
  private val defaultCreateRoomSucceededFormat = Json.format[ServerProtocol.CreateRoomSucceeded]
  private val defaultJoinSucceededFormat = Json.format[ServerProtocol.JoinSucceeded]
  private val defaultFuturePredicateFinishedFormat = Json.format[ServerProtocol.FuturePredicateFinished]
  private val defaultDeclareBidChangeAvailableNoticeFormat = Json.format[ServerProtocol.DeclareBidChangeAvailableNotice]
  private val defaultDeclareBidChangingNoticeFormat = Json.format[ServerProtocol.DeclareBidChangingNotice]
  private val defaultNewPlayerJoinedFormat = Json.format[ServerProtocol.NewPlayerJoined]
  private val defaultMasterLeaveFormat = Json.format[ServerProtocol.MasterLeave]
  private val defaultPlayerLeaveFormat = Json.format[ServerProtocol.PlayerLeave]
  private val defaultGameStartedFormat = Json.format[ServerProtocol.GameStarted]
  private val defaultNewRoundStarted = Json.format[ServerProtocol.NewRoundStarted]
  private val defaultBidDeclaredFormat = Json.format[ServerProtocol.BidDeclared]
  private val defaultTrickPhaseStartedFormat = Json.format[ServerProtocol.TrickPhaseStarted]
  private val defaultPlayedFormat = Json.format[ServerProtocol.Played]
  private val defaultTigressPlayed = Json.format[ServerProtocol.TigressPlayed]
  private val defaultRascalOfRoatanPlayed = Json.format[ServerProtocol.RascalOfRoatanPlayed]
  private val defaultTrickFinishedFormat = Json.format[ServerProtocol.TrickFinished]
  private val defaultNextTrickLeadPlayerChangeableNotice = Json.format[ServerProtocol.NextTrickLeadPlayerChangeableNotice]
  private val defaultNextTrickLeadPlayerChangingNotice = Json.format[ServerProtocol.NextTrickLeadPlayerChangingNotice]
  private val defaultHandChangeAvailableNotice = Json.format[ServerProtocol.HandChangeAvailableNotice]
  private val defaultHandChangingNotice = Json.format[ServerProtocol.HandChangingNotice]
  private val defaultGotBonusScore = Json.format[ServerProtocol.GotBonusScore]
  private val defaultFuturePredicated = Json.format[ServerProtocol.FuturePredicated]
  private val defaultFuturePredicatedOtherPlayer = Json.format[ServerProtocol.FuturePredicatedOtherPlayer]
  private val defaultDeclareBidChanged = Json.format[ServerProtocol.DeclareBidChanged]
  private val defaultNextPlayFormat = Json.format[ServerProtocol.NextPlay]
  private val defaultRoundFinishedFormat = Json.format[ServerProtocol.RoundFinished]
  private val defaultGameFinishedFormat = Json.format[ServerProtocol.GameFinished]
  private val defaultNextTrickChangedFormat = Json.format[ServerProtocol.NextTrickChanged]
  private val defaultHandChangedFormat = Json.format[ServerProtocol.HandChanged]
  private val defaultReplayGameFormat = Json.format[ServerProtocol.ReplayGame]
  private val defaultShutdownGameFormat = Json.format[ServerProtocol.ShutdownGame]

  private val format = new Format[ServerProtocol] {
    override def reads(json: JsValue): JsResult[ServerProtocol] =
      (json \ "event").validate[String].flatMap {
        case "connected" => defaultConnectedFormat.reads(json)
        case "disconnect" => defaultDisconnectFormat.reads(json)
        case "disconnected" => defaultDisconnectedFormat.reads(json)
        case "createRoomSucceeded" => defaultCreateRoomSucceededFormat.reads(json)
        case "joinSucceeded" => defaultJoinSucceededFormat.reads(json)
        case "masterLeave" => defaultMasterLeaveFormat.reads(json)
        case "futurePredicateFinished" => defaultFuturePredicateFinishedFormat.reads(json)
        case "declareBidChangeAvailableNotice" => defaultDeclareBidChangeAvailableNoticeFormat.reads(json)
        case "declareBidChangingNotice" => defaultDeclareBidChangingNoticeFormat.reads(json)
        case "newPlayerJoined" => defaultNewPlayerJoinedFormat.reads(json)
        case "playerLeave" => defaultPlayerLeaveFormat.reads(json)
        case "gameStarted" => defaultGameStartedFormat.reads(json)
        case "newRoundStarted" => defaultNewRoundStarted.reads(json)
        case "bidDeclared" => defaultBidDeclaredFormat.reads(json)
        case "trickPhaseStarted" => defaultTrickPhaseStartedFormat.reads(json)
        case "played" => defaultPlayedFormat.reads(json)
        case "tigressPlayed" => defaultTigressPlayed.reads(json)
        case "rascalOfRoatanPlayed" => defaultRascalOfRoatanPlayed.reads(json)
        case "trickFinished" => defaultTrickFinishedFormat.reads(json)
        case "nextTrickLeadPlayerChangeableNotice" => defaultNextTrickLeadPlayerChangeableNotice.reads(json)
        case "nextTrickLeadPlayerChangingNotice" => defaultNextTrickLeadPlayerChangingNotice.reads(json)
        case "handChangeAvailableNotice" => defaultHandChangeAvailableNotice.reads(json)
        case "handChangingNotice" => defaultHandChangingNotice.reads(json)
        case "gotBonusScore" => defaultGotBonusScore.reads(json)
        case "futurePredicated" => defaultFuturePredicated.reads(json)
        case "futurePredicatedOtherPlayer" => defaultFuturePredicatedOtherPlayer.reads(json)
        case "declareBidChanged" => defaultDeclareBidChanged.reads(json)
        case "nextPlay" => defaultNextPlayFormat.reads(json)
        case "roundFinished" => defaultRoundFinishedFormat.reads(json)
        case "gameFinished" => defaultGameFinishedFormat.reads(json)
        case "nextTrickChanged" => defaultNextTrickChangedFormat.reads(json)
        case "handChanged" => defaultHandChangedFormat.reads(json)
        case "replayGame" => defaultReplayGameFormat.reads(json)
        case "shutdownGame" => defaultShutdownGameFormat.reads(json)
      }

    override def writes(input: ServerProtocol): JsValue =
      input match {
        case i: ServerProtocol.Connected => defaultConnectedFormat.writes(i)
        case i: ServerProtocol.Disconnect => defaultDisconnectFormat.writes(i)
        case i: ServerProtocol.Disconnected => defaultDisconnectedFormat.writes(i)
        case i: ServerProtocol.CreateRoomSucceeded => defaultCreateRoomSucceededFormat.writes(i)
        case i: ServerProtocol.JoinSucceeded => defaultJoinSucceededFormat.writes(i)
        case i: ServerProtocol.FuturePredicateFinished => defaultFuturePredicateFinishedFormat.writes(i)
        case i: ServerProtocol.DeclareBidChangeAvailableNotice => defaultDeclareBidChangeAvailableNoticeFormat.writes(i)
        case i: ServerProtocol.DeclareBidChangingNotice => defaultDeclareBidChangingNoticeFormat.writes(i)
        case i: ServerProtocol.NewPlayerJoined => defaultNewPlayerJoinedFormat.writes(i)
        case i: ServerProtocol.MasterLeave => defaultMasterLeaveFormat.writes(i)
        case i: ServerProtocol.PlayerLeave => defaultPlayerLeaveFormat.writes(i)
        case i: ServerProtocol.GameStarted => defaultGameStartedFormat.writes(i)
        case i: ServerProtocol.NewRoundStarted => defaultNewRoundStarted.writes(i)
        case i: ServerProtocol.BidDeclared => defaultBidDeclaredFormat.writes(i)
        case i: ServerProtocol.TrickPhaseStarted => defaultTrickPhaseStartedFormat.writes(i)
        case i: ServerProtocol.Played => defaultPlayedFormat.writes(i)
        case i: ServerProtocol.TigressPlayed => defaultTigressPlayed.writes(i)
        case i: ServerProtocol.RascalOfRoatanPlayed => defaultRascalOfRoatanPlayed.writes(i)
        case i: ServerProtocol.TrickFinished => defaultTrickFinishedFormat.writes(i)
        case i: ServerProtocol.NextTrickLeadPlayerChangeableNotice => defaultNextTrickLeadPlayerChangeableNotice.writes(i)
        case i: ServerProtocol.NextTrickLeadPlayerChangingNotice => defaultNextTrickLeadPlayerChangingNotice.writes(i)
        case i: ServerProtocol.HandChangeAvailableNotice => defaultHandChangeAvailableNotice.writes(i)
        case i: ServerProtocol.HandChangingNotice => defaultHandChangingNotice.writes(i)
        case i: ServerProtocol.GotBonusScore => defaultGotBonusScore.writes(i)
        case i: ServerProtocol.FuturePredicated => defaultFuturePredicated.writes(i)
        case i: ServerProtocol.FuturePredicatedOtherPlayer => defaultFuturePredicatedOtherPlayer.writes(i)
        case i: ServerProtocol.DeclareBidChanged => defaultDeclareBidChanged.writes(i)
        case i: ServerProtocol.NextPlay => defaultNextPlayFormat.writes(i)
        case i: ServerProtocol.RoundFinished => defaultRoundFinishedFormat.writes(i)
        case i: ServerProtocol.GameFinished => defaultGameFinishedFormat.writes(i)
        case i: ServerProtocol.NextTrickChanged => defaultNextTrickChangedFormat.writes(i)
        case i: ServerProtocol.HandChanged => defaultHandChangedFormat.writes(i)
        case i: ServerProtocol.ReplayGame => defaultReplayGameFormat.writes(i)
        case i: ServerProtocol.ShutdownGame => defaultShutdownGameFormat.writes(i)
      }
  }

}