package org.cafienne.board.actorapi.command.definition.column

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.command.definition.{BoardDefinitionCommand, UpdateFormElement}
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionUpdated
import org.cafienne.board.actorapi.response.BoardResponse
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class UpdateColumnDefinition(val user: BoardUser, val columnId: String, val title: Option[String], val form: Option[ValueMap]) extends BoardDefinitionCommand(user) with UpdateFormElement {
  /**
    * Method to be implemented to handle the command.
    *
    * @param board
    * @return
    */
  override def process(board: BoardActor): Unit = {
    // TODO: Verify the column exists

    // Check if title or form is to be updated and has actual changes
    if (titleChanged(board.getDefinition.getTitle) || formChanged(board.getDefinition.getStartForm)) {
      board.addEvent(new ColumnDefinitionUpdated(board, columnId, title, form))
    }
    setResponse(new BoardResponse(this))
  }

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.columnId, columnId)
    writeField(generator, Fields.title, title)
    writeField(generator, Fields.form, form)
  }
}

object UpdateColumnDefinition {
  def deserialize(json: ValueMap): UpdateColumnDefinition = {
    UpdateColumnDefinition(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.columnId), json.readOption[String](Fields.title), json.readOptionalMap(Fields.form))
  }
}
