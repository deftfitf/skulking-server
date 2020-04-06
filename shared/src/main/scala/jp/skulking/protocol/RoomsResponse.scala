package jp.skulking.protocol

import play.api.libs.json.Json

case class RoomsResponse(rooms: List[String])

object RoomsResponse {

  implicit val defaultRoomsResponseFormat = Json.format[RoomsResponse]

}