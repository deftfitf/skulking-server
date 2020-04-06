package jp.skulking.protocol

import play.api.libs.json.{Format, JsResult, JsValue, Json}

trait ClientProtocolJSONFormats {

  def deserialize(jsValue: JsValue): ClientProtocol = {
    ClientProtocolJSONFormats.format.reads(jsValue).get
  }

  def serialize(clientProtocol: ClientProtocol): JsValue = {
    ClientProtocolJSONFormats.format.writes(clientProtocol)
  }

}

object ClientProtocolJSONFormats {

  private val defaultConnectFormat = Json.format[ClientProtocol.Connect]
  private val defaultDisconnectFormat = Json.format[ClientProtocol.Disconnect]
  private val defaultCreateRoomFormat = Json.format[ClientProtocol.CreateRoom]
  private val defaultJoinFormat = Json.format[ClientProtocol.Join]
  private val defaultLeaveFormat = Json.format[ClientProtocol.Leave]
  private val defaultStartFormat = Json.format[ClientProtocol.Start]
  private val defaultBidDeclareFormat = Json.format[ClientProtocol.BidDeclare]
  private val defaultPlayTrickFormat = Json.format[ClientProtocol.PlayTrick]
  private val defaultPlayTigressFormat = Json.format[ClientProtocol.PlayTigress]
  private val defaultPlayRascalOfRoatan = Json.format[ClientProtocol.PlayRascalOfRoatan]
  private val defaultChangeDealerFormat = Json.format[ClientProtocol.ChangeDealer]
  private val defaultChangeHandFormat = Json.format[ClientProtocol.ChangeHand]
  private val defaultFuturePredicateFinishFormat = Json.format[ClientProtocol.FuturePredicateFinish]
  private val defaultBidDeclareChangeFormat = Json.format[ClientProtocol.BidDeclareChange]
  private val defaultReplayFormat = Json.format[ClientProtocol.Replay]
  private val defaultFinishFormat = Json.format[ClientProtocol.Finish]

  private val format = new Format[ClientProtocol] {
    override def reads(json: JsValue): JsResult[ClientProtocol] =
      (json \ "event").validate[String].flatMap {
        case "connect" => defaultConnectFormat.reads(json)
        case "disconnect" => defaultDisconnectFormat.reads(json)
        case "createRoom" => defaultCreateRoomFormat.reads(json)
        case "join" => defaultJoinFormat.reads(json)
        case "leave" => defaultLeaveFormat.reads(json)
        case "start" => defaultStartFormat.reads(json)
        case "bidDeclare" => defaultBidDeclareFormat.reads(json)
        case "playTrick" => defaultPlayTrickFormat.reads(json)
        case "playTigress" => defaultPlayTigressFormat.reads(json)
        case "playRascalOfRoatan" => defaultPlayRascalOfRoatan.reads(json)
        case "changeDealer" => defaultChangeDealerFormat.reads(json)
        case "changeHand" => defaultChangeHandFormat.reads(json)
        case "futurePredicateFinish" => defaultFuturePredicateFinishFormat.reads(json)
        case "bidDeclareChange" => defaultBidDeclareChangeFormat.reads(json)
        case "replay" => defaultReplayFormat.reads(json)
        case "finish" => defaultFinishFormat.reads(json)
      }

    override def writes(input: ClientProtocol): JsValue =
      input match {
        case i: ClientProtocol.Connect => defaultConnectFormat.writes(i)
        case i: ClientProtocol.Disconnect => defaultDisconnectFormat.writes(i)
        case i: ClientProtocol.CreateRoom => defaultCreateRoomFormat.writes(i)
        case i: ClientProtocol.Join => defaultJoinFormat.writes(i)
        case i: ClientProtocol.Leave => defaultLeaveFormat.writes(i)
        case i: ClientProtocol.Start => defaultStartFormat.writes(i)
        case i: ClientProtocol.BidDeclare => defaultBidDeclareFormat.writes(i)
        case i: ClientProtocol.PlayTrick => defaultPlayTrickFormat.writes(i)
        case i: ClientProtocol.PlayTigress => defaultPlayTigressFormat.writes(i)
        case i: ClientProtocol.PlayRascalOfRoatan => defaultPlayRascalOfRoatan.writes(i)
        case i: ClientProtocol.ChangeDealer => defaultChangeDealerFormat.writes(i)
        case i: ClientProtocol.ChangeHand => defaultChangeHandFormat.writes(i)
        case i: ClientProtocol.FuturePredicateFinish => defaultFuturePredicateFinishFormat.writes(i)
        case i: ClientProtocol.BidDeclareChange => defaultBidDeclareChangeFormat.writes(i)
        case i: ClientProtocol.Replay => defaultReplayFormat.writes(i)
        case i: ClientProtocol.Finish => defaultFinishFormat.writes(i)
      }
  }

}