package org.cafienne.board.actorapi.command.definition.column

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.actorapi.command.definition.BoardDefinitionCommand
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionRemoved
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class RemoveColumnDefinition(val user: BoardUser, val columnId: String) extends BoardDefinitionCommand(user) {
  override def validate(definition: BoardDefinition): Unit = {
    super.validate(definition)
    if (!definition.columns.exists(_.columnId == columnId)) {
      // Cannot find column definition, let's return an error
      throw new InvalidCommandException("Board does not have a column with id " + columnId)
    }
  }

  override def processBoardDefinitionCommand(definition: BoardDefinition): Unit = {
    definition.addEvent(new ColumnDefinitionRemoved(definition, columnId))
  }

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.columnId, columnId)
  }
}

object RemoveColumnDefinition {
  def deserialize(json: ValueMap): RemoveColumnDefinition = {
    RemoveColumnDefinition(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.columnId))
  }
}
