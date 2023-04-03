package org.cafienne.board.actorapi.command.team

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.consentgroup.actorapi.ConsentGroupMember
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class SetMember(user: BoardUser, member: ConsentGroupMember) extends BoardTeamCommand(user) {
  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.member, member)
  }
}

object SetMember {
  def deserialize(json: ValueMap): SetMember = {
    SetMember(BoardUser.deserialize(json.readMap(Fields.user)), ConsentGroupMember.deserialize(json.readMap(Fields.member)))
  }
}
