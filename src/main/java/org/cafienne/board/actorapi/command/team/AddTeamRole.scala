package org.cafienne.board.actorapi.command.team

import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class AddTeamRole(user: BoardUser, roleName: String) extends BoardTeamRoleCommand(user, roleName)

object AddTeamRole {
  def deserialize(json: ValueMap): AddTeamRole = {
    AddTeamRole(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.role))
  }
}
