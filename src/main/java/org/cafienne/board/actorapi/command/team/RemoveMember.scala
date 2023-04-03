package org.cafienne.board.actorapi.command.team

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class RemoveMember(user: BoardUser, memberId: String) extends BoardTeamMemberCommand(user) {
  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.member, memberId)
  }
}

object RemoveMember {
  def deserialize(json: ValueMap): RemoveMember = {
    RemoveMember(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.member))
  }
}
