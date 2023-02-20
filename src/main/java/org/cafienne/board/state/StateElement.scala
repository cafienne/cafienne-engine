package org.cafienne.board.state

import org.cafienne.board.BoardActor
import org.cafienne.cmmn.instance.debug.DebugInfoAppender

trait StateElement {
  val board: BoardActor

  def addDebugInfo(appender: DebugInfoAppender): Unit = {
    board.addDebugInfo(appender)
  }
}
