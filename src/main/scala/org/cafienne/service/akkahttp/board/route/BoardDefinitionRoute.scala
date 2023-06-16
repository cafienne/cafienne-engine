/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.service.akkahttp.board.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.service.akkahttp.board.model.BoardAPI._
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/board")
class BoardDefinitionRoute(override val caseSystem: CaseSystem) extends BoardRoute {

  override def routes: Route = concat(addColumn, removeColumn, addOrReplaceTaskForm, addOrReplaceStartForm, createBoard)

  @Path("/")
  @POST
  @Operation(
    summary = "Create a Board",
    description = "Creates a new board to overview the work on tasks",
    tags = Array("board"),
    responses = Array(
      new ApiResponse(description = "Board created or replaced successfully", responseCode = "204"),
      new ApiResponse(description = "Board information is invalid", responseCode = "400"),
    )
  )
  @RequestBody(description = "Board to create or update", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[BoardRequestDetails]))))
  @Consumes(Array("application/json"))
  def createBoard: Route = post {
    boardUser { boardUser =>
      entity(as[BoardRequestDetails]) { newTenantInformation => //TODO entity as BoardRequestDetails
        complete(StatusCodes.Created)
        //askBoard(new ReplaceTenant(tenantOwner, tenantOwner.tenant, users.asJava))
      }
    }
  }

  @Path("/{board}/form")
  @POST
  @Operation(
    summary = "Add or replace a board start form",
    description = "Add or replace a board start form - a custom form that is provided to be filled when a case instance starts",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to manipulate", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Form updated successfully", responseCode = "204"),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  ) //TODO make serializer for the form definition (kind of JSValue?)
  @RequestBody(description = "Form to create", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[String]))))
  @Consumes(Array("application/json"))
  def addOrReplaceStartForm: Route = post {
    boardUser { boardUser =>
      path(Segment / "form") { boardId =>
        entity(as[String]) { newForm =>
          if (!boardUser.isOwner) {
            complete(StatusCodes.Unauthorized, "Only bord managers  can change forms for boards")
          } else {
            complete(StatusCodes.NotImplemented)
            //askModelActor(new CreateConsentGroup(groupOwner, group))
          }
        }
      }
    }
  }

  @Path("/{board}/columns")
  @POST
  @Operation(
    summary = "Add a column",
    description = "Add a column to the board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board in which to add the column", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Column added successfully", responseCode = "204"),
      new ApiResponse(description = "Column information is invalid", responseCode = "400"),
    )
  ) //TODO make class and serializer for the ColumnDetails
  @RequestBody(description = "Column details", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[ColumnRequestDetails]))))
  @Consumes(Array("application/json"))
  def addColumn: Route =
    post  {
      boardUser { boardUser =>
        path(Segment / "columns") { boardId =>
          entity(as[String]) { newUser => //entity as ColumnRequestDetails
            complete(StatusCodes.NotImplemented)
            //askBoard(new SetTenantUser(tenantOwner, tenantOwner.tenant, newUser.asTenantUser(tenantOwner.tenant)))
          }
        }
    }
  }

  @Path("/{board}/columns/{columnId}")
  @DELETE
  @Operation(
    summary = "Remove a column",
    description = "Removes the column from the board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board from which to remove the column", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "columnId", description = "The id of the column", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Column  removed successfully", responseCode = "204"),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  def removeColumn: Route = delete {
    boardUser { boardUser =>
      path(Segment / "columns" / Segment) { (boardId, columnId ) =>
        complete(StatusCodes.NotImplemented)
        //askBoard(new RemoveTenantUser(tenantOwner, tenantOwner.tenant, userId))
      }
    }
  }

  @Path("/{board}/columns/{columnId}/form")
  @POST
  @Operation(
    summary = "Add or replace a column form",
    description = "Add or replace a column form - a custom form that is provided with the active task in the flow",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "board", description = "The board to manipulate", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "columnId", description = "The id of the column", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Column form updated successfully", responseCode = "204"),
      new ApiResponse(responseCode = "404", description = "Board not found"),
      new ApiResponse(responseCode = "404", description = "Column not found"),
    )
  ) //TODO attach serialized form (JSValue like)
  @RequestBody(description = "Form to add or replace", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[String]))))
  @Consumes(Array("application/json"))
  def addOrReplaceTaskForm: Route = post {
    boardUser { boardUser =>
      path(Segment / "columns" / Segment / "form") { (boardId, columnId) =>
        entity(as[String]) { newForm =>
          if (!boardUser.isOwner) {
            complete(StatusCodes.Unauthorized, "Only board managers can manage column forms")
          } else {
            complete(StatusCodes.NotImplemented)
            //askModelActor(new CreateConsentGroup(groupOwner, group))
          }
        }
      }
    }
  }
}
