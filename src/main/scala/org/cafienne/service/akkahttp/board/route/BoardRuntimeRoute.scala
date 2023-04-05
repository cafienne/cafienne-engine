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
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.response.{ActorExistsFailure, CommandFailure, EngineChokedFailure, SecurityFailure}
import org.cafienne.board.BoardFields
import org.cafienne.board.actorapi.command.runtime.GetBoard
import org.cafienne.board.actorapi.response.runtime.GetBoardResponse
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.board.state.team.BoardTeam
import org.cafienne.infrastructure.akkahttp.route.LastModifiedDirectives
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.querydb.query.filter.TaskFilter
import org.cafienne.querydb.query.{TaskQueries, TaskQueriesImpl}
import org.cafienne.service.akkahttp.Headers
import org.cafienne.service.akkahttp.board.model.{BoardAPI, BoardTeamAPI}
import org.cafienne.system.CaseSystem

import javax.ws.rs._
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/boards")
class BoardRuntimeRoute(override val caseSystem: CaseSystem) extends BoardRoute with LastModifiedDirectives {
  val taskQueries: TaskQueries = new TaskQueriesImpl

  override def routes: Route = concat(getBoards, getBoard)

  @Path("/")
  @GET
  @Operation(
    summary = "Get the boards",
    description = "Retrieves the list of boards",
    tags = Array("board"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of available boards", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[BoardAPI.BoardSummaryResponse]))))),
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

  @Path("/{boardId}")
  @GET
  @Operation(
    summary = "Get the board and its details",
    description = "Retrieves Board, Columns, Tasks and team for a specific board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "boardId", description = "The id of the board to retrieve", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array( //TODO return a detailed Board type with all required fields
      new ApiResponse(responseCode = "200", description = "Board and its details", content = Array(new Content(schema = new Schema(implementation = classOf[BoardAPI.Examples.BoardResponse])))),
      new ApiResponse(responseCode = "404", description = "Board not found"),
    )
  )
  @Produces(Array("application/json"))
  def getBoard: Route = get {
    readLastModifiedHeader(Headers.BOARD_LAST_MODIFIED) { lastModified =>
      boardUser { boardUser =>
        val command = new GetBoard(boardUser)

        val businessIdentifier = Some(s"${BoardDefinition.BOARD_IDENTIFIER}=${boardUser.boardId}")
        val query = for {
          commandResponse <- caseSystem.gateway.request(command)
          tasks <- {
            // If last modified is filled, we can use that to await case updates.
            // But then - that can only be done if last modified is not on the board itself
            //  The reason behind this: if a board is created and updated, it gives last modified of board itself (i.e., timestamp with board id),
            //  but if a task in a flow in the board is updated, that is actually something done on a case. In such a scenario, the case id with it's last timestamp
            //  is set in the response header ("BOARD_LAST_MODIFIED"), and then the actor id of last modified is not equal to the board id.
            //  So in that case, we can run a synced query against the CaseReader.lastModifiedRegistration.
            if (lastModified.lastModified.map(_.actorId).getOrElse(boardUser.boardId) == boardUser.boardId) {
              taskQueries.getAllTasks(boardUser, TaskFilter(identifiers = businessIdentifier))
            } else {
              runSyncedQuery(taskQueries.getAllTasks(boardUser, TaskFilter(identifiers = businessIdentifier)), lastModified)
            }
          }
          consentGroup <- {
            if (lastModified.lastModified.map(_.actorId).getOrElse(boardUser.boardId) == boardUser.boardId) {
              userQueries.getConsentGroup(boardUser, boardUser.boardId + BoardTeam.EXTENSION)
            } else {
              runSyncedQuery(userQueries.getConsentGroup(boardUser, boardUser.boardId + BoardTeam.EXTENSION), lastModified)
            }
          }
        } yield (commandResponse, tasks, consentGroup)

        onComplete(query) {
          case Success(result) =>
            val response = result._1
            val tasks = result._2
            val group = result._3
            response match {
              case e: CommandFailure => response match {
                case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
                case _: EngineChokedFailure => complete(StatusCodes.InternalServerError, "An error happened in the server; check the server logs for more information")
                case e: ActorExistsFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
                case _ => complete(StatusCodes.BadRequest, e.exception.getMessage)
              }
              case value: GetBoardResponse => {
                val definition = value.state.definition
                val team = {
                  val members = group.members.toSeq.map(BoardTeamAPI.TeamMemberFormat.fromMember)
                  val roles = value.state.team.roles.toSet
                  BoardTeamAPI.BoardTeamFormat(roles, members)
                }
                val columns: Seq[BoardAPI.Column] = definition.columns.toSeq.map(column => {
                  val columnTasks: Seq[BoardAPI.Task] = tasks.filter(_.isActive).filter(column.getTitle == _.taskName).map(task => {
                    val taskData = task.inputJson.merge(task.outputJson).asMap()
                    val data = taskData.readMap(BoardFields.Data)
                    val subject = taskData.readMap(BoardFields.BoardMetadata).readString(Fields.subject, "")
                    BoardAPI.Task(id = task.id, subject = Some(subject), flowId = task.caseInstanceId,
                      claimedBy = Some(task.assignee), form = task.getJSON(task.taskModel).asMap(), data = data)
                  })
                  BoardAPI.Column(column.columnId, column.position, Some(column.getTitle), Some(column.getRole), tasks = columnTasks)
                })

                completeJson(BoardAPI.BoardResponseFormat(definition.boardId, Some(definition.getTitle), team, columns))
              }
              case other => // Unknown new type of response that is not handled
                logger.error(s"Received an unexpected response after asking CaseSystem a command of type ${command.getCommandDescription}. Response is of type ${other.getClass.getSimpleName}")
                complete(StatusCodes.OK)
            }
          case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
        }
      }
    }
  }
}
