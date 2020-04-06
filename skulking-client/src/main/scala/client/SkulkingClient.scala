package client

import client.scene.{Dashboard, FrameId}

object SkulkingClient {

  def main(args: Array[String]): Unit = {
    val frameId = FrameId("skulking-board")
    Dashboard.transition(frameId)
  }

}
