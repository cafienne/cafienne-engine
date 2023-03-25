/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.board.route

import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.board.actorapi.command.team.SetBoardTeam
import org.cafienne.board.state.definition.TeamDefinition
import org.cafienne.service.akkahttp.LastModifiedHeader
import org.cafienne.service.akkahttp.board.model.BoardTeamAPI
import org.cafienne.system.CaseSystem

import javax.ws.rs._
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/board")
class BoardTeamRoute(override val caseSystem: CaseSystem) extends BoardRoute {

  override def routes: Route = concat(getTeam, setTeam)

  @Path("/{board}/team")
  @GET
  @Operation(
    summary = "Get the team of this board",
    description = "Retrieves the definition of a board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to retrieve the team from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "The team for this board", content = Array(new Content(schema = new Schema(implementation = classOf[BoardTeamAPI.BoardTeam])))),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  @Produces(Array("application/json"))
  def getTeam: Route = get {
    boardUser { boardUser =>
      path("team") {
        onComplete(runSyncedQuery(userQueries.getConsentGroup(boardUser, boardUser.boardId + TeamDefinition.EXTENSION), LastModifiedHeader.NONE)) {
          case Success(group) =>
            val team = BoardTeamAPI.BoardTeam(roles = group.groupRoles, members = group.members.map(m => BoardTeamAPI.TeamMemberDetails(m.userId, None, m.roles)))
            completeJson(team)
          case Failure(f) => throw f
        }
      }
    }
  }

  @Path("/{board}/team")
  @POST
  @Operation(
    summary = "Add or replace team",
    description = "Add or replace the team that is connected to this board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board in where the team needs to change", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Team adapted successfully", responseCode = "204"),
      new ApiResponse(description = "Team information is invalid", responseCode = "400"),
    )
  )
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[BoardTeamAPI.BoardTeam]))))
  @Consumes(Array("application/json"))
  def setTeam: Route = {
    boardUser { boardUser =>
      path("team") {
        entity(as[BoardTeamAPI.BoardTeam]) { newUser => //entity as TeamMemberDetails
          println("New team: " + newUser)
          askBoard(new SetBoardTeam(boardUser))
        }
      }
    }
  }

}