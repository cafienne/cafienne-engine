package org.cafienne.board.actorapi.command.definition.column

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.command.definition.BoardDefinitionCommand
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionRemoved
import org.cafienne.board.actorapi.response.BoardResponse
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class RemoveColumnDefinition(val user: BoardUser, val columnId: String) extends BoardDefinitionCommand(user) {
  /**
    * Method to be implemented to handle the command.
    *
    * @param board
    * @return
    */
  override def process(board: BoardActor): Unit = {
    // TODO: Verify the column exists

    // Check if title or form is to be updated and has actual changes
    if (!board.getDefinition.columns.exists(_.columnId == columnId)) {
      // Cannot find column definition, let's return an error
      throw new InvalidCommandException("Board does not have a column with id " + columnId)
    }
    board.addEvent(new ColumnDefinitionRemoved(board, columnId))
    setResponse(new BoardResponse(this))
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
