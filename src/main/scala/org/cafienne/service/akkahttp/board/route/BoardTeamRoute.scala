/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.board.route

import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.board.actorapi.command.team.{AddTeamRole, RemoveMember, RemoveTeamRole, SetMember}
import org.cafienne.service.akkahttp.board.model.BoardTeamAPI
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/board")
class BoardTeamRoute(override val caseSystem: CaseSystem) extends BoardRoute {

  override def routes: Route = concat(addMember, replaceMember, removeMember, addRole, removeRole)

  @Path("/{board}/team/members")
  @POST
  @Operation(
    summary = "Add a new board team member",
    description = "Add the new member to the board team",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to retrieve the team from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "The member is being added to the team of this board"),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[BoardTeamAPI.TeamMemberFormat]))))
  def addMember: Route = post {
    boardUser { boardUser =>
      path("team" / "members") {
        entity(as[BoardTeamAPI.TeamMemberFormat]) { newTeamMember => //entity as TeamMemberDetails
          askBoard(new SetMember(boardUser, newTeamMember.asMember()))
        }
      }
    }
  }

  @Path("/{board}/team/members/{memberId}")
  @POST
  @Operation(
    summary = "Replace the board team member",
    description = "Replace the roles and/or the name of this member in the board team",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to replace the member in", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "memberId", description = "The user id of the member to replace", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "The member information is being replaced"),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[BoardTeamAPI.TeamMemberFormat]))))
  def replaceMember: Route = post {
    boardUser { boardUser =>
      path("team" / "members" / Segment) { memberId =>
        entity(as[BoardTeamAPI.ReplaceTeamMemberFormat]) { newTeamMember => //entity as TeamMemberDetails
          println("Replacing team member: " + newTeamMember)
          askBoard(new SetMember(boardUser, newTeamMember.asMember(memberId)))
        }
      }
    }
  }

  @Path("/{board}/team/members/{memberId}")
  @DELETE
  @Operation(
    summary = "Remove this member from the board team",
    description = "Remove this member from the board team",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to remove the member from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "memberId", description = "The user id of the member to remove", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "The member is being removed"),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  def removeMember: Route = delete {
    boardUser { boardUser =>
      path("team" / "members" / Segment) { memberId =>
        askBoard(new RemoveMember(boardUser, memberId))
      }
    }
  }

  @Path("/{board}/team/roles/{roleName}")
  @PUT
  @Operation(
    summary = "Add a role to the board team",
    description = "Add a role to the board team",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to replace the member in", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "roleName", description = "The team role to add", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "The role is being added"),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  def addRole: Route = put {
    boardUser { boardUser =>
      path("team" / "roles" / Segment) { role =>
        askBoard(new AddTeamRole(boardUser, role))
      }
    }
  }

  @Path("/{board}/team/roles/{roleName}")
  @DELETE
  @Operation(
    summary = "Remove a role from the board team",
    description = "Remove the role from the board team",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to remove the member from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "roleName", description = "The team role to remove", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "The role is being removed"),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  def removeRole: Route = delete {
    boardUser { boardUser =>
      path("team" / "roles" / Segment) { role =>
        askBoard(new RemoveTeamRole(boardUser, role))
      }
    }
  }
}