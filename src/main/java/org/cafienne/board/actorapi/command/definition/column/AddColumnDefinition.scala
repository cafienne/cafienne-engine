package org.cafienne.board.actorapi.command.definition.column

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.actormodel.response.ActorLastModified
import org.cafienne.board.actorapi.command.definition.BoardDefinitionCommand
import org.cafienne.board.actorapi.event.definition.ColumnDefinitionAdded
import org.cafienne.board.actorapi.response.{BoardResponse, ColumnAddedResponse}
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class AddColumnDefinition(val user: BoardUser, val columnId: String, val title: String, val role: Option[String], val form: Option[ValueMap]) extends BoardDefinitionCommand(user) {
  override def validate(definition: BoardDefinition): Unit = {
    super.validate(definition)
    if (definition.columns.exists(_.columnId == columnId)) {
      throw new InvalidCommandException("A column with id " + columnId + " already exists")
    }
  }

  override def processBoardDefinitionCommand(definition: BoardDefinition): Unit = {
    if (role.nonEmpty) {
      definition.upsertTeamRole(role.get)
    }
    definition.addEvent(new ColumnDefinitionAdded(definition, columnId, title, role.getOrElse(""), form.getOrElse(new ValueMap())))
  }

  override def createResponse(lastModified: ActorLastModified): BoardResponse = new ColumnAddedResponse(this, columnId, lastModified)

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.columnId, columnId)
    writeField(generator, Fields.title, title)
    writeField(generator, Fields.role, role)
    writeField(generator, Fields.form, form)
  }
}

object AddColumnDefinition {
  def deserialize(json: ValueMap): AddColumnDefinition = {
    AddColumnDefinition(user = BoardUser.deserialize(json.readMap(Fields.user)), columnId = json.readString(Fields.columnId), title = json.readString(Fields.title), role = json.readOption[String](Fields.role), form = json.readOptionalMap(Fields.form))
  }
}
