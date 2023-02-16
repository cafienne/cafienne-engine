package org.cafienne.board.actorapi.command.definition.column

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.command.definition.BoardDefinitionCommand
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionAdded
import org.cafienne.board.actorapi.response.ColumnAddedResponse
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class AddColumnDefinition(val user: BoardUser, val columnId: String, val title: String, val form: Option[ValueMap]) extends BoardDefinitionCommand(user) {
  override def validate(board: BoardActor): Unit = {
    super.validate(board)
    if (board.getDefinition.columns.exists(_.columnId == columnId)) {
      throw new InvalidCommandException("A column with id " + columnId + " already exists")
    }
  }

  override def process(board: BoardActor): Unit = {
    board.addEvent(new ColumnDefinitionAdded(board, columnId, title, form))
    setResponse(new ColumnAddedResponse(this, columnId))
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
