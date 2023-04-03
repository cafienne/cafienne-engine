package org.cafienne.board.actorapi.command.team

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.actormodel.response.ModelResponse
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.response.BoardResponse
import org.cafienne.infrastructure.serialization.Fields

class BoardTeamRoleCommand(user: BoardUser, roleName: String) extends BoardTeamCommand(user) {
  override def process(board: BoardActor): ModelResponse = {
    super.process(board)
    new BoardResponse(this)
  }

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
    writeField(generator, Fields.role, roleName)
  }
}
