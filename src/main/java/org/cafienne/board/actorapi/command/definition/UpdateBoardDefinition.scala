package org.cafienne.board.actorapi.command.definition

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.actorapi.event.definition.BoardDefinitionUpdated
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class UpdateBoardDefinition(user: BoardUser, override val title: Option[String], override val form: Option[ValueMap]) extends BoardDefinitionCommand(user) with UpdateFormElement {
  override def processBoardDefinitionCommand(definition: BoardDefinition): Unit = {
    // Check if title or form is to be updated and has actual changes
    runChangeDetector(definition, (newTitle, newForm, _) => new BoardDefinitionUpdated(definition, newTitle, newForm))
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
