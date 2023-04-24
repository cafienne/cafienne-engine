package org.cafienne.board.actorapi.command.definition.role

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.actorapi.command.team.BoardTeamMemberCommand
import org.cafienne.infrastructure.serialization.Fields

class RoleDefinitionCommand(user: BoardUser, roleName: String) extends BoardTeamMemberCommand(user) {
  override protected def isAsync: Boolean = false

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.role, roleName)
  }
}
