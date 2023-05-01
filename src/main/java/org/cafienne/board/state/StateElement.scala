package org.cafienne.board.state

import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.actormodel.response.ModelResponse
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.event.BoardEvent
import org.cafienne.cmmn.instance.debug.DebugInfoAppender

import scala.concurrent.{Future, Promise}
import scala.util.Try

trait StateElement {
  val state: BoardState
  def board: BoardActor = state.board // Can be null
  val boardId: String = state.boardId

  def addDebugInfo(appender: DebugInfoAppender): Unit = {
    board.addDebugInfo(appender)
  }

  def askModelActor(command: ModelCommand): Future[ModelResponse] = {
    val promise: Promise[ModelResponse] = Promise[ModelResponse]()
    board.askModel(command, failure => promise.complete(Try(failure)), success => promise.complete(Try(success)))
    promise.future
  }

  def addEvent[E <: BoardEvent](event: E): E = board.addEvent(event)
}
