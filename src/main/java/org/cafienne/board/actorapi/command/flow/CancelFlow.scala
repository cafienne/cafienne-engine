package org.cafienne.board.actorapi.command.flow

import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class CancelFlow(user: BoardUser, private val flow: String) extends BoardFlowCommand(user, flow)

object CancelFlow {
  def deserialize(json: ValueMap): CancelFlow = CancelFlow(BoardUser.deserialize(json.readMap(Fields.user)), json.readString(Fields.flowId))
}
