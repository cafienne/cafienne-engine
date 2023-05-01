package org.cafienne.board.actorapi.command.definition.column

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.actorapi.command.definition.{BoardDefinitionCommand, UpdateFormElement}
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionUpdated
import org.cafienne.board.state.definition.{BoardDefinition, ColumnDefinition}
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class UpdateColumnDefinition(val user: BoardUser, val columnId: String, override val title: Option[String], override val role: Option[String], override val form: Option[ValueMap])
  extends BoardDefinitionCommand(user) with UpdateFormElement {

  override def validate(definition: BoardDefinition): Unit = {
    super.validate(definition)
    if (!definition.columns.exists(_.columnId == columnId)) {
      throw new InvalidCommandException("Board does not have a column with id " + columnId)
    }
  }

  override def processBoardDefinitionCommand(definition: BoardDefinition): Unit = {
    // TODO: Verify the column exists

    // Check if title or form is to be updated and has actual changes
    val columnDefinition: ColumnDefinition = definition.columns.find(_.columnId == columnId).getOrElse({
      // Cannot find column definition, let's return an error
      /// actually done already in the validate ... but also do it here, just in case
      throw new InvalidCommandException("Board does not have a column with id " + columnId)
    })

    if (roleChanged(columnDefinition.getRole) && role.nonEmpty) {
      definition.upsertTeamRole(role.get)
    }
    runChangeDetector(columnDefinition, (newTitle, newForm, newRole) => new ColumnDefinitionUpdated(definition, columnId, newTitle, newRole, newForm))
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
