package org.cafienne.board.actorapi.command.definition.role

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.actorapi.command.definition.BoardDefinitionCommand
import org.cafienne.infrastructure.serialization.Fields

abstract class RoleDefinitionCommand(user: BoardUser, roleName: String) extends BoardDefinitionCommand(user) {
  override protected def isAsync: Boolean = false

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.role, roleName)
  }
}
