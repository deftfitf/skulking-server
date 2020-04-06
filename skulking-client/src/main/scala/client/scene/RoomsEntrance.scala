package client.scene

import client.component.PopUpInterfaceComponent
import client.{ActorSystemModule, HtmlClassDef, jquery}
import jp.skulking.domain.{CardDecks, DeckId, RoomId, SessionId}
import jp.skulking.header.SessionHeader
import jp.skulking.protocol.RoomsResponse
import org.scalajs.dom.Element
import org.scalajs.dom.html.Div
import org.scalajs.jquery.{JQueryEventObject, JQueryXHR}
import play.api.libs.json.Json
import scalatags.JsDom.all._

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

final class RoomsEntrance(
                           frameId: FrameId,
                           roomsEndpoint: String,
                           sessionId: SessionId,
                           popUpInterfaceComponent: PopUpInterfaceComponent) {

  private val roomsId = "skulking-rooms"

  private val createRoomButton: Div =
    div(
      button(cls := HtmlClassDef.ROOMS_CREATE_ROOM_BUTTON, "部屋作成")
    ).render

  private val roundRadioName = "create-room-round-radio"
  private val cardRuleRadioName = "create-room-rule-radio"
  private val maxRoomSizeNRadioName = "create-room-max-room-size-radio"

  private val createRoomSubmitButton =
    button(`type` := "button", "作成する").render
  private val createRoomCancelButton =
    button(`type` := "button", "キャンセル").render

  private val createRoomPopup: Div =
    div(
      cls := "create-room-popup",
      p("部屋を作成します"),
      div(
        label("ラウンド数"),
        div(
          cls := "round-radio-div",
          (1 to 10).map(r => {
            div(
              input(`type` := "radio", name := roundRadioName, value := r) ::
                label(cls := "round-radio-label", r) :: Nil
            )
          })
        )
      ),
      div(
        label("ルール"),
        div(
          cls := "rule-radio-div",
          CardDecks.decks.keys.map(d => {
            div(
              input(`type` := "radio", name := cardRuleRadioName, value := d.value) ::
                label(cls := "rule-radio-label", d.value) :: Nil
            )
          }).toList)
      ),
      div(
        label("最大人数"),
        div(
          cls := "member-radio-div",
          (2 to 6).map { n =>
            div(
              input(`type` := "radio", name := maxRoomSizeNRadioName, value := n) ::
                label(cls := "member-radio-label", n) :: Nil
            )
          }
        )
      ),
      createRoomSubmitButton, createRoomCancelButton
    ).render
  jquery(createRoomPopup).hide()

  jquery(createRoomButton).bind("click", (_: Element, _: JQueryEventObject) => {
    jquery(createRoomPopup).show()

    jquery(createRoomSubmitButton).bind("click", (_: Element, _: JQueryEventObject) => {
      val nOfRound: Int = jquery(s"""input:radio[name="$roundRadioName"]:checked""").value().asInstanceOf[String].toInt
      val deckId: String = jquery(s"""input:radio[name="$cardRuleRadioName"]:checked""").value().asInstanceOf[String]
      val roomMaxSize: Int = jquery(s"""input:radio[name="$maxRoomSizeNRadioName"]:checked""").value().asInstanceOf[String].toInt
      ActorSystemModule.spawn(
        SkulkingBoardSceneActor.createRoom(frameId, sessionId, nOfRound, DeckId(deckId), roomMaxSize),
        "create-room")
    })

    jquery(createRoomCancelButton).bind("click", (_: Element, _: JQueryEventObject) => {
      jquery(createRoomPopup).hide()
    })
  })

  val self: Div =
    div(
      id := frameId.id,
      popUpInterfaceComponent.self,
      div(
        cls := HtmlClassDef.ROOMS_LOADING_BOX + " skulking-container-dashboard",
        div(
          cls := "skulking-container-center",
          p("部屋一覧を読み込んでいます"),
          createRoomPopup,
          createRoomButton,
          div(
            id := roomsId,
            cls := "skulking-container-center",
            div(cls := HtmlClassDef.ROOMS_LOADING_ICON),
          )
        )
      )
    ).render

  private def listRooms(roomIds: List[RoomId]): Div =
    div(
      id := frameId.id,
      popUpInterfaceComponent.self,
      div(
        cls := "skulking-container-dashboard",
        div(
          cls := "skulking-container-center",
          createRoomPopup,
          createRoomButton,
          div(
            id := roomsId,
            ul(
              ((cls := HtmlClassDef.ROOMS_LIST_UL): Modifier) +:
                roomIds.map(id => {
                  val lst = li(cls := HtmlClassDef.ROOMS_LIST_UL_LI, s"room-${id.value}").render
                  jquery(lst).bind("click", (_: Element, _: JQueryEventObject) => {
                    popUpInterfaceComponent.yesNoPopup(
                      s"${id.value}に入室しますか?", "はい", "いいえ",
                      {
                        ActorSystemModule.spawn(
                          SkulkingBoardSceneActor.joinRoom(frameId, sessionId, id),
                          "join-room")
                      }, {
                      }
                    )
                  })
                  lst: Modifier
                }): _*
            )
          )
        )
      )
    ).render

  @ScalaJSDefined
  trait RoomsResponseRaw extends js.Any {
    val rooms: UndefOr[js.Array[Long]] = js.undefined
  }

  def loadRoom(): Unit =
    jquery.ajax(
      js.Dynamic.literal(
        url = roomsEndpoint,
        method = "GET",
        headers = js.Dynamic.literal(
          SessionHeader.name -> sessionId.value),
        dataType = "text"
      ).asInstanceOf[org.scalajs.jquery.JQueryAjaxSettings]
    ).done((data: String, _: String, _: JQueryXHR) => {
      val roomsResponse = RoomsResponse.defaultRoomsResponseFormat.reads(Json.parse(data)).get
      frameId.transition(listRooms(roomsResponse.rooms.map(RoomId.apply)))
    }).fail((_: JQueryXHR, _: String, _: String) => {
      popUpInterfaceComponent.yesPopup(
        "部屋一覧の取得に失敗しました", "再接続", {
          loadRoom()
        })
    })

}

object RoomsEntrance {

  private val ROOMS_ENDPOINT = "http://localhost:9000/rooms"

  def transition(frameId: FrameId, sessionId: SessionId): Unit = {
    val popUpInterfaceComponent = PopUpInterfaceComponent(frameId.id)
    jquery(popUpInterfaceComponent.self).hide()
    val roomEntrance = new RoomsEntrance(
      frameId,
      ROOMS_ENDPOINT,
      sessionId,
      popUpInterfaceComponent)
    roomEntrance.loadRoom()

    frameId.$.replaceWith(roomEntrance.self)
  }

}