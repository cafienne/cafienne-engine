package org.cafienne.board.actorapi.command.definition

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.actormodel.response.ModelResponse
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.event.definition.BoardDefinitionUpdated
import org.cafienne.board.actorapi.response.BoardResponse
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class UpdateBoardDefinition(val user: BoardUser, val title: Option[String], val form: Option[ValueMap]) extends BoardDefinitionCommand(user) with UpdateDefinition {
  val role = None
  /**
    * Method to be implemented to handle the command.
    *
    * @param board
    * @return
    */
  override def process(board: BoardActor): ModelResponse = {
    // Check if title or form is to be updated and has actual changes
    if (titleChanged(board.getDefinition.getTitle) || formChanged(board.getDefinition.getStartForm)) {
      board.addEvent(new BoardDefinitionUpdated(board, title, form))
    } else {
//      System.out.println("Board has no updates for title or form")
    }
    new BoardResponse(this)
  }

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.title, title)
    writeField(generator, Fields.form, form)
  }
}

object UpdateBoardDefinition {
  def deserialize(json: ValueMap): UpdateBoardDefinition = {
    UpdateBoardDefinition(BoardUser.deserialize(json.readMap(Fields.user)), json.readOption(Fields.title), json.readOptionalMap(Fields.form))
  }
}
