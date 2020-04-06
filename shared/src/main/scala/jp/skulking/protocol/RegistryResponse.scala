package jp.skulking.protocol

import play.api.libs.json.{Format, Json}

case class RegistryResponse(sessionId: Option[String], reason: Option[String])

object RegistryResponse {

  implicit val defaultRegistryResponseFormat: Format[RegistryResponse] = Json.format[RegistryResponse]

}