package jp.skulking.protocol

import play.api.libs.json.{Format, Json}

case class LoginResponse(sessionId: Option[String], reason: Option[String])

object LoginResponse {

  implicit val defaultLoginResponseFormat: Format[LoginResponse] = Json.format[LoginResponse]

}
