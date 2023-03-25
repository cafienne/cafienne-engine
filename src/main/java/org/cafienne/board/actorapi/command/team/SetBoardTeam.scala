package org.cafienne.board.actorapi.command.team

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.identity.BoardUser
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.response.BoardResponse
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap

@Manifest
case class SetBoardTeam(user: BoardUser) extends BoardTeamMemberCommand(user) {
  /**
    * Method to be implemented to handle the command.
    *
    * @param board
    * @return
    */
  override def process(board: BoardActor): Unit = {
    setResponse(new BoardResponse(this))
  }

  override def write(generator: JsonGenerator): Unit = {
    super.writeModelCommand(generator)
  }
}

object SetBoardTeam {
  def deserialize(json: ValueMap): SetBoardTeam = {
    SetBoardTeam(BoardUser.deserialize(json.readMap(Fields.user)))
  }
}
