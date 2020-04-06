package client.scene

import client.jquery
import org.scalajs.dom.html.Div
import org.scalajs.jquery.JQuery

final case class FrameId(id: String) {

  def $: JQuery = jquery("#" + id)

  def transition(sceneDiv: Div): Unit =
    $.replaceWith(sceneDiv)

}
