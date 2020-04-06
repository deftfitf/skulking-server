package client.scene

import client.component.PopUpInterfaceComponent
import client.jquery
import jp.skulking.domain.SessionId
import jp.skulking.protocol.{LoginRequest, RegistryRequest}
import org.scalajs.dom.Element
import org.scalajs.dom.html.{Div, Form, Input}
import org.scalajs.jquery.{JQueryEventObject, JQueryXHR}
import scalatags.JsDom.all._

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

final class Dashboard(
                       frameId: FrameId,
                       loginEndpoint: String,
                       registryEndpoint: String,
                       popUpInterfaceComponent: PopUpInterfaceComponent) {

  private val loginFormId = "skulking-login-form"

  private val loginButtonId = "skulking-login-button"
  private val loginButton = button(id := loginButtonId, `type` := "button", "ログイン").render

  private val registryButtonId = "skulking-registry-button"
  private val registryButton = button(id := registryButtonId, `type` := "button", "登録").render

  private val playerIdInput: Input = input(`type` := "text").render
  private val playerPasswordInput: Input = input(`type` := "password").render

  private val loginForm: Form =
    form(
      id := loginFormId,
      div(
        label("プレイヤーID"), playerIdInput,
        br,
        label("パスワード"), playerPasswordInput
      ),
      loginButton,
      registryButton
    ).render

  @ScalaJSDefined
  trait LoginResponseRaw extends js.Object {
    val sessionId: UndefOr[String] = js.undefined
  }

  jquery(loginButton).bind("click", (_: Element, _: JQueryEventObject) => {
    val playerId = jquery(playerIdInput).value().asInstanceOf[String]
    val password = jquery(playerPasswordInput).value().asInstanceOf[String]
    jquery.ajax(
      js.Dynamic.literal(
        url = loginEndpoint,
        method = "POST",
        contentType = "application/json",
        data = LoginRequest
          .defaultLoginRequestFormat
          .writes(LoginRequest(playerId, password))
          .toString(),
        dataType = "json"
      ).asInstanceOf[org.scalajs.jquery.JQueryAjaxSettings]
    ).done((o: js.Any, _: String, _: JQueryXHR) => {
      val loginResponseRaw = o.asInstanceOf[LoginResponseRaw]
      loginResponseRaw.sessionId.toOption match {
        case Some(sessionId) => RoomsEntrance.transition(frameId, SessionId(sessionId))
        case None =>
          popUpInterfaceComponent.yesPopup(
            s"ログインに失敗しました ユーザー名とパスワードを再確認してください", "はい", {})
      }
    }).fail((_: JQueryXHR, _: String, _: String) => {
      popUpInterfaceComponent.yesPopup(
        "ログインに失敗しました", "はい", {})
    })
  })

  @ScalaJSDefined
  trait RegistryResponseRaw extends js.Object {
    val sessionId: UndefOr[String] = js.undefined
    val reason: UndefOr[String] = js.undefined
  }

  jquery(registryButton).bind("click", (_: Element, _: JQueryEventObject) => {
    val playerId = jquery(playerIdInput).value().asInstanceOf[String]
    val password = jquery(playerPasswordInput).value().asInstanceOf[String]
    jquery.ajax(
      js.Dynamic.literal(
        url = registryEndpoint,
        method = "POST",
        contentType = "application/json",
        data = RegistryRequest
          .defaultRegistryRequestFormat
          .writes(RegistryRequest(playerId, password))
          .toString(),
        dataType = "json"
      ).asInstanceOf[org.scalajs.jquery.JQueryAjaxSettings]
    ).done((o: js.Any, _: String, _: JQueryXHR) => {
      val loginResponseRow = o.asInstanceOf[RegistryResponseRaw]
      loginResponseRow.sessionId.toOption match {
        case Some(sessionId) => RoomsEntrance.transition(frameId, SessionId(sessionId))
        case None =>
          popUpInterfaceComponent.yesPopup(
            s"新規登録に失敗しました ${loginResponseRow.reason.get}", "はい", {})
      }
    }).fail((_: JQueryXHR, _: String, _: String) => {
      popUpInterfaceComponent.yesPopup(
        "新規登録に失敗しました ユーザー名とパスワードを再確認してください", "はい", {})
    })
  })

  val self: Div =
    div(
      id := frameId.id,
      popUpInterfaceComponent.self,
      div(
        cls := "skulking-container-dashboard",
        div(
          cls := "skulking-container-center",
          p("ここから先はログインが必要です"),
          loginForm
        )
      )
    ).render

}

object Dashboard {

  private val DASHBOARD_LOGIN_ENDPOINT = "http://localhost:9000/login"
  private val DASHBOARD_REGISTRY_ENDPOINT = "http://localhost:9000/registry"

  def transition(frameId: FrameId): Unit = {
    val popUpInterfaceComponent = PopUpInterfaceComponent(frameId.id)
    jquery(popUpInterfaceComponent.self).hide()
    val dashboard = new Dashboard(
      frameId,
      DASHBOARD_LOGIN_ENDPOINT,
      DASHBOARD_REGISTRY_ENDPOINT,
      popUpInterfaceComponent)

    frameId.$.replaceWith(dashboard.self)
  }

}