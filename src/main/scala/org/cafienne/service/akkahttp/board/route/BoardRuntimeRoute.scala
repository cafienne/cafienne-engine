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
import org.cafienne.actormodel.response.{ActorExistsFailure, CommandFailure, EngineChokedFailure, SecurityFailure}
import org.cafienne.board.BoardFields
import org.cafienne.board.actorapi.command.runtime.GetBoard
import org.cafienne.board.actorapi.response.runtime.GetBoardResponse
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.infrastructure.akkahttp.route.LastModifiedDirectives
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.querydb.query.filter.TaskFilter
import org.cafienne.querydb.query.{TaskQueries, TaskQueriesImpl}
import org.cafienne.service.akkahttp.board.model.BoardAPI
import org.cafienne.service.akkahttp.board.model.BoardAPI._
import org.cafienne.system.CaseSystem

import javax.ws.rs._
import scala.collection.immutable.HashSet
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/board")
class BoardRuntimeRoute(override val caseSystem: CaseSystem) extends BoardRoute with LastModifiedDirectives {
  val taskQueries: TaskQueries = new TaskQueriesImpl

  import org.cafienne.querydb.query.board.BoardQueryProtocol._

  override def routes: Route = concat(getBoards, getTeam, getBoard, addTeam)

  @Path("/")
  @GET
  @Operation(
    summary = "Get the boards",
    description = "Retrieves the list of boards",
    tags = Array("board"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of available boards", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[BoardSummaryResponse]))))),
    )
  )
  @Produces(Array("application/json"))
  def getBoards: Route = get {
    authenticatedUser { user =>
      pathEndOrSingleSlash {
        //TODO something like: boardQueries.getBoards(boardUser.toString)
        // NOTE that this response should be of BoardSummaryResponse (giving a selection of the data available)
        complete(StatusCodes.NotImplemented, user.toString)
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
      new ApiResponse(responseCode = "200", description = "Board and its details", content = Array(new Content(schema = new Schema(implementation = classOf[BoardResponseFormat])))),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  @Produces(Array("application/json"))
  def getBoard: Route = get {
    boardUser { boardUser =>
      val command = new GetBoard(boardUser)

      val businessIdentifier = Some(s"${BoardDefinition.BOARD_IDENTIFIER}=${boardUser.boardId}")

      val query = for {
        commandResponse <- caseSystem.gateway.request(command)
        // TODO: Somehow make it run SyncedQuery with the response last modifieds?
        //    Yes. Idea: it should use BOARD_LAST_MODIFIED and Board should give most latest CaseLastModified as it's last modified
//        tasks <- runSyncedQuery(taskQueries.getAllTasks(boardUser, TaskFilter(identifiers = businessIdentifier)))

        tasks <- taskQueries.getAllTasks(boardUser, TaskFilter(identifiers = businessIdentifier))
      } yield (commandResponse, tasks)

      onComplete(query) {
        case Success(result) =>
          val response = result._1
          val tasks = result._2
          response match {
            case e: CommandFailure => response match {
              case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
              case _: EngineChokedFailure => complete(StatusCodes.InternalServerError, "An error happened in the server; check the server logs for more information")
              case e: ActorExistsFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
              case _ => complete(StatusCodes.BadRequest, e.exception.getMessage)
            }
            case value: GetBoardResponse => {
              val definition = value.definition
              val team = definition.team.users.toSeq.map(user => TeamMemberDetails(userId = user.id, name = Some(s"Name of ${user.id}"), roles = HashSet[String]()))
              val columns: Seq[BoardAPI.Column] = definition.columns.toSeq.map(column => {
                val columnTasks: Seq[BoardAPI.Task] = tasks.filter(column.getTitle == _.taskName).map(task => {
                  val taskInput = task.getJSON(task.input).asMap()
                  val data = taskInput.readMap(BoardFields.Data)
                  val subject = taskInput.readMap(BoardFields.BoardMetadata).readString(Fields.subject, "")
                  BoardAPI.Task(id = task.id, subject = Some(subject), flowId = task.caseInstanceId,
                    claimedBy = Some(task.assignee), form = task.getJSON(task.taskModel).asMap(), data = data)
                })
                BoardAPI.Column(column.columnId, column.position, Some(column.getTitle), Some(column.getRole), tasks = columnTasks)
              })

              completeJson(BoardResponseFormat(definition.boardId, Some(definition.getTitle), team, columns))
            }
            case other => // Unknown new type of response that is not handled
              logger.error(s"Received an unexpected response after asking CaseSystem a command of type ${command.getCommandDescription}. Response is of type ${other.getClass.getSimpleName}")
              complete(StatusCodes.OK)
          }
        case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)      }
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
      new ApiResponse(responseCode = "204", description = "The team for this board", content = Array(new Content(schema = new Schema(implementation = classOf[Set[TeamMember]])))),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  @Produces(Array("application/json"))
  def getTeam: Route = get {
    boardUser { boardUser =>
      path("team") {
        val team = Seq(
          TeamMemberDetails(boardUser.id, Some("Board User 1"), Set("BOARD_MANAGER")),
          TeamMemberDetails("userId2", Some("Board User 2"), Set("INTAKE_ROLE")),
        )
        completeJson(team)
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
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TeamMemberDetails]))))
  @Consumes(Array("application/json"))
  def addTeam: Route = {
    boardUser { boardUser =>
      path("team") {
        entity(as[String]) { newUser => //entity as TeamMemberDetails
          complete(StatusCodes.NotImplemented)
          //askBoard(new SetTenantUser(tenantOwner, tenantOwner.tenant, newUser.asTenantUser(tenantOwner.tenant)))
        }
      }
    }
  }

}
