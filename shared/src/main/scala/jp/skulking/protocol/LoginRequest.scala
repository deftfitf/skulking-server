package jp.skulking.protocol

import play.api.libs.json.{Format, Json}

case class LoginRequest(playerId: String, password: String)

object LoginRequest {

  implicit val defaultLoginRequestFormat: Format[LoginRequest] = Json.format[LoginRequest]

}
