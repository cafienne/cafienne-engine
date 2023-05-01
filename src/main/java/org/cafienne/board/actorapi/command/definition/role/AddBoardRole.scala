package org.cafienne.board.actorapi.command.definition.role

import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class AddBoardRole(user: BoardUser, roleName: String) extends RoleDefinitionCommand(user, roleName) {
  override protected def processBoardDefinitionCommand(definition: BoardDefinition): Unit = definition.upsertTeamRole(roleName)
}

object AddBoardRole {
  def deserialize(json: ValueMap): AddBoardRole = {
    AddBoardRole(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.role))
  }
}
