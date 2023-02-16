package org.cafienne.board.actorapi.command.definition

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.actormodel.response.ModelResponse
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionAdded
import org.cafienne.board.actorapi.response.ColumnAddedResponse
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class AddColumnDefinition(val user: BoardUser, val columnId: String, val title: String, val form: Option[ValueMap]) extends BoardDefinitionCommand(user) {
  /**
    * Method to be implemented to handle the command.
    *
    * @param board
    * @return
    */
  override def process(board: BoardActor): ModelResponse = {
    board.addEvent(new ColumnDefinitionAdded(board, columnId, title, form))
    new ColumnAddedResponse(this, columnId)
  }

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.columnId, columnId)
    writeField(generator, Fields.title, title)
    writeField(generator, Fields.form, form)
  }
}

object AddColumnDefinition {
  def deserialize(json: ValueMap): AddColumnDefinition = {
    AddColumnDefinition(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.columnId), json.readString(Fields.title), json.readOptionalMap(Fields.form))
  }
}
