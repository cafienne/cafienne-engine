package org.cafienne.board.actorapi.command.definition.column

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.command.definition.{BoardDefinitionCommand, UpdateFormElement}
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionUpdated
import org.cafienne.board.actorapi.response.BoardResponse
import org.cafienne.board.state.definition.ColumnDefinition
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class UpdateColumnDefinition(val user: BoardUser, val columnId: String, val title: Option[String], val role: Option[String], val form: Option[ValueMap]) extends BoardDefinitionCommand(user) with UpdateFormElement {
  /**
    * Method to be implemented to handle the command.
    *
    * @param board
    * @return
    */
  override def process(board: BoardActor): Unit = {
    // TODO: Verify the column exists

    // Check if title or form is to be updated and has actual changes
    val definition: ColumnDefinition = board.getDefinition.columns.find(_.columnId == columnId).getOrElse({
      // Cannot find column definition, let's return an error
      throw new InvalidCommandException("Board does not have a column with id " + columnId)
    })
    val hasTitleChange = titleChanged(definition.getTitle)
    val hasRoleChange = roleChanged(definition.getRole)
    val hasFormChange = formChanged(definition.getForm)
    if (hasTitleChange || hasRoleChange || hasFormChange) {
      if (hasRoleChange && role.nonEmpty) {
        board.getDefinition.team.upsertTeamRole(role.get)
      }
      board.addEvent(new ColumnDefinitionUpdated(board, columnId, title, role, form))
    }
    setResponse(new BoardResponse(this))
  }

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.columnId, columnId)
    writeField(generator, Fields.title, title)
    writeField(generator, Fields.role, role)
    writeField(generator, Fields.form, form)
  }
}

object UpdateColumnDefinition {
  def deserialize(json: ValueMap): UpdateColumnDefinition = {
    UpdateColumnDefinition(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.columnId), json.readOption[String](Fields.title), json.readOption[String](Fields.role), json.readOptionalMap(Fields.form))
  }
}
