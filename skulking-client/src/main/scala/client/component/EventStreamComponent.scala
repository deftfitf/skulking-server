package client.component

import client.jquery
import org.scalajs.dom.html.Div
import scalatags.JsDom.all._

final case class EventStreamComponent(parentId: String) {
  private val selfId = s"$parentId-eventstream"
  val self: Div = div(
    h3("イベント"),
    div(id := selfId)
  ).render
  private val MAX_HISTORY = 20
  private[this] var count = 0

  def consume(event: String): Unit = {
    if (count < MAX_HISTORY) {
      jquery("#" + selfId).prepend(div(p(event)).render)
      count += 1
    } else {
      jquery("#" + selfId).children().last().remove()
      jquery("#" + selfId).prepend(div(p(event)).render)
    }
  }

}
