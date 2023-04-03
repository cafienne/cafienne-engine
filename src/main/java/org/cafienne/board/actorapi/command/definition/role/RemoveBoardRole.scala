package org.cafienne.board.actorapi.command.definition.role

import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class RemoveBoardRole(user: BoardUser, roleName: String) extends RoleDefinitionCommand(user, roleName)

object RemoveBoardRole {
  def deserialize(json: ValueMap): RemoveBoardRole = {
    RemoveBoardRole(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.role))
  }
}