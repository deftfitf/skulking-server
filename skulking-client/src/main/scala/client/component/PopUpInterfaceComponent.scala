package client.component

import client.{HtmlClassDef, jquery}
import org.scalajs.dom.Element
import org.scalajs.dom.html.Div
import org.scalajs.jquery.JQueryEventObject
import scalatags.JsDom.all._

final case class PopUpInterfaceComponent(parentId: String) {
  private val selfId = s"$parentId-popup"
  val self: Div = div(
    id := selfId
  ).render

  def yesNoPopup(message: String, yes: String, no: String, onYes: => Unit, onNo: => Unit): Unit = {
    val yesButton = button(cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_YES, yes).render
    val noButton = button(cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_NO, no).render

    jquery(yesButton).bind("click", (_: Element, _: JQueryEventObject) => {
      clear()
      onYes
    })
    jquery(noButton).bind("click", (_: Element, _: JQueryEventObject) => {
      clear()
      onNo
    })

    clear()
    jquery("#" + selfId).replaceWith(
      div(
        id := selfId,
        div(message),
        div(
          cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_BOX,
          yesButton,
          noButton
        )
      ).render).show()
  }

  def yesPopup(message: String, yes: String, onYes: => Unit): Unit = {
    val yesButton = button(cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_YES, yes).render
    jquery(yesButton).bind("click", (_: Element, _: JQueryEventObject) => {
      clear()
      onYes
    })

    clear()
    jquery("#" + selfId).replaceWith(
      div(
        id := selfId,
        div(message),
        div(
          cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_BOX,
          yesButton
        )
      ).render).show()
  }

  def rangeButtonPopup(message: String, min: Int, max: Int, onClick: Int => Unit): Unit = {
    val bidButtons =
      (min to max).map(bid => {
        val bidButton = button(cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_YES, bid).render
        jquery(bidButton).bind("click", (_: Element, _: JQueryEventObject) => {
          clear()
          onClick(bid)
        })
        bidButton: Modifier
      })

    clear()
    jquery("#" + selfId).replaceWith(
      div(
        id := selfId,
        div(message),
        div(
          ((cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_BOX): Modifier) +:
            bidButtons: _*
        )).render).show()
  }

  def multiSelectPopup(message: String, selects: (String, () => Unit)*): Unit = {
    val multiSelects =
      selects.map(select => {
        val selectButton = button(cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_YES, select._1).render
        jquery(selectButton).bind("click", (_: Element, _: JQueryEventObject) => {
          clear()
          select._2()
        })
        selectButton: Modifier
      })

    clear()
    jquery("#" + selfId).replaceWith(
      div(
        id := selfId,
        div(message),
        div(
          ((cls := HtmlClassDef.POPUP_INTERFACE_BUTTON_BOX): Modifier) +:
            multiSelects: _*
        )).render).show()
  }

  def clear(): Unit = {
    jquery("#" + selfId).empty().hide()
  }
}
