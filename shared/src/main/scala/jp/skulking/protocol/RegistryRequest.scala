package jp.skulking.protocol

import play.api.libs.json.{Format, Json}

case class RegistryRequest(playerId: String, password: String)

object RegistryRequest {

  implicit val defaultRegistryRequestFormat: Format[RegistryRequest] = Json.format[RegistryRequest]

}
