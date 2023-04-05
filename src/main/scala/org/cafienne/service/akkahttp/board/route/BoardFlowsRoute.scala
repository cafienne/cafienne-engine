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
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.board.actorapi.command.flow._
import org.cafienne.json.ValueMap
import org.cafienne.service.akkahttp.board.model.FlowAPI._
import org.cafienne.system.CaseSystem
import org.cafienne.util.Guid

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/boards")
class BoardFlowsRoute(override val caseSystem: CaseSystem) extends BoardRoute {
  override def routes: Route = concat(startFlow, completeFlowTask, claimFlowTask, saveFlowTask)

  @Path("{boardId}/flows")
  @POST
  @Operation(
    summary = "Start a flow in the board",
    description = "Start a flow in the board",
    tags = Array("board"),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "Flow Started", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[FlowStartedFormat]))))),
    )
  )
  @RequestBody(description = "Board to create or update", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[StartFlowFormat]))))
  @Produces(Array("application/json"))
  def startFlow: Route = post {
    boardUser { user =>
      path("flows") {
        pathEndOrSingleSlash {
          entity(as[StartFlowFormat]) { flow =>
            askFlow(new StartFlow(user, flow.id.getOrElse(new Guid().toString), flow.subject, asJson(flow.data).getOrElse(new ValueMap())))
          }
        }
      }
    }
  }

  @Path("{boardId}/flows/{flowId}/tasks/{taskId}/claim")
  @PUT
  @Operation(
    summary = "Claim a task in a flow on the board",
    description = "Claim a task in a flow on the board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "boardId", description = "The id of the board", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "flowId", description = "The id of the flow containing the task", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "taskId", description = "The id of the task to claim", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "Task claimed"),
    )
  )
  @Produces(Array("application/json"))
  def claimFlowTask: Route = put {
    // TODO: SWAGGER document the path parameters
    boardUser { user =>
      path("flows" / Segment / "tasks" / Segment / "claim") { (flowId, taskId) =>
        pathEndOrSingleSlash {
          askFlow(new ClaimFlowTask(user, flowId, taskId))
        }
      }
    }
  }

  @Path("{boardId}/flows/{flowId}/tasks/{taskId}")
  @PUT
  @Operation(
    summary = "Save a task in a flow on the board",
    description = "Save a task in a flow on the board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "boardId", description = "The id of the board", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "flowId", description = "The id of the flow containing the task", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "taskId", description = "The id of the task to save", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "Task completion initiated"),
    )
  )
  @RequestBody(description = "Task to save", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[FlowTaskOutputFormat]))))
  @Produces(Array("application/json"))
  def saveFlowTask: Route = put {
    boardUser { user =>
      path("flows" / Segment / "tasks" / Segment) { (flowId, taskId) =>
        pathEndOrSingleSlash {
          entity(as[FlowTaskOutputFormat]) { output =>
            askFlow(new SaveFlowTaskOutput(user, flowId, taskId, output.subject, asJson(output.data)))
          }
        }
      }
    }
  }

  @Path("{boardId}/flows/{flowId}/tasks/{taskId}")
  @POST
  @Operation(
    summary = "Complete a task in a flow on the board",
    description = "Complete a task in a flow on the board",
    tags = Array("board"),
    parameters = Array(
      new Parameter(name = "boardId", description = "The id of the board", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "flowId", description = "The id of the flow containing the task", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "taskId", description = "The id of the task to complete", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "Task completion initiated"),
    )
  )
  @RequestBody(description = "Task to complete", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[FlowTaskOutputFormat]))))
  @Produces(Array("application/json"))
  def completeFlowTask: Route = post {
    boardUser { user =>
      path("flows" / Segment / "tasks" / Segment) { (flowId, taskId) =>
        pathEndOrSingleSlash {
          entity(as[FlowTaskOutputFormat]) { output =>
            askFlow(new CompleteFlowTask(user, flowId, taskId, output.subject, asJson(output.data)))
          }
        }
      }
    }
  }

  def askFlow(command: BoardFlowCommand): Route = askBoard(command)
}
