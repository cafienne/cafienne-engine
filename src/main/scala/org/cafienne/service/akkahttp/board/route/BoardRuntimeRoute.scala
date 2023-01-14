/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.board.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/board")
class BoardRuntimeRoute(override val caseSystem: CaseSystem) extends BoardRoute {
  import org.cafienne.querydb.query.board.BoardQueryProtocol._
  override def routes: Route = concat(getBoards, getTeam, getBoard, addTeam, putTeam)

  @Path("/")
  @GET
  @Operation(
    summary = "Get the boards",
    description = "Retrieves the list of boards",
    tags = Array("board"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of available boards", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[Board]))))),
    )
  )
  @Produces(Array("application/json"))
  def getBoards: Route = get {
    boardUser { boardUser =>
      pathEnd {
        //TODO something like: boardQueries.getBoards(boardUser.toString)
        complete(StatusCodes.NotImplemented)
      }
    }
  }


  @Path("/{board}")
  @GET
  @Operation(
    summary = "Get the board and its details",
    description = "Retrieves Board, Columns, Tasks and team for a specific board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to retrieve details from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array( //TODO return a detailed Board type with all required fields
      new ApiResponse(responseCode = "200", description = "Board and its details", content = Array(new Content(schema = new Schema(implementation = classOf[Set[String]])))),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  @Produces(Array("application/json"))
  def getBoard: Route = get {
    pathPrefix(Segment) { boardId =>
        //TODO something like boardQueries.getBoard(boardId)
        complete(StatusCodes.NotImplemented)
      }
    }


  @Path("/{board}/team")
  @GET
  @Operation(
    summary = "Get the team of this board",
    description = "Retrieves the definition of a board",
    tags = Array("board", "team"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to retrieve the team from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array( //TODO have a team return type
      new ApiResponse(responseCode = "204", description = "The team for this board", content = Array(new Content(schema = new Schema(implementation = classOf[Set[String]])))),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  @Produces(Array("application/json"))
  def getTeam: Route = get {
    boardUser { boardUser =>
      path("team") {
        complete(StatusCodes.NotImplemented)
      }
    }
  }

  @Path("/{board}/team")
  @POST
  @Operation(
    summary = "Add or replace team",
    description = "Add or replace the team that is connected to this board",
    tags = Array("board", "team"),
    parameters = Array(
      new Parameter(name = "board", description = "The board in where the team needs to change", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Team adapted successfully", responseCode = "204"),
      new ApiResponse(description = "Team information is invalid", responseCode = "400"),
    )
  ) //TODO have a Team input format
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[String]))))
  @Consumes(Array("application/json"))
  def addTeam: Route = post {
    replaceTeam
  }

  // For compatibility continue to support PUT for some time on the same
  def putTeam: Route = put {
    replaceTeam
  }

  def replaceTeam: Route = {
    boardUser { boardUser =>
      path("team") {
        entity(as[String]) { newUser =>
          complete(StatusCodes.NotImplemented)
          //askBoard(new SetTenantUser(tenantOwner, tenantOwner.tenant, newUser.asTenantUser(tenantOwner.tenant)))
        }
      }
    }
  }


}
