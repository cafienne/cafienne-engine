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

package org.cafienne.service.akkahttp.tasks

import org.apache.pekko.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.humantask.actorapi.command._
import org.cafienne.infrastructure.akkahttp.HttpJsonReader._
import org.cafienne.json.ValueMap
import org.cafienne.service.akkahttp.tasks.TaskAPIFormat._
import org.cafienne.system.CaseSystem

import jakarta.ws.rs._

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/tasks")
class TaskActionRoutes(override val caseSystem: CaseSystem) extends TaskRoute {
  override def routes: Route = concat(validateTaskOutput, saveTaskOutput, claimTaskRoute, revokeTaskRoute, assignTaskRoute, delegateTaskRoute, completeTaskRoute)

  @Path("/{taskId}")
  @POST
  @Operation(
    summary = "Validate task output parameters",
    description = "Validate task output parameters",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "taskId", description = "Unique id of the task", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Task output validated successfully", responseCode = "200"),
      new ApiResponse(description = "Task output invalid", responseCode = "400"),
      new ApiResponse(description = "Task not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Task output to be validated", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Examples.TaskOutputFormat]))))
  @Produces(Array("application/json"))
  def validateTaskOutput: Route = post {
    caseUser { user =>
      path(Segment) { taskId =>
        entity(as[ValueMap]) {
          outputParams => askTask(user, taskId, (caseInstanceId, tenantUser) => new ValidateTaskOutput(tenantUser, caseInstanceId, taskId, outputParams))
        }
      }
    }
  }

  @Path("/{taskId}")
  @PUT
  @Operation(
    summary = "Save task output parameters",
    description = "Save task output parameters",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "taskId", description = "The id of the task", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Task updated successfully", responseCode = "202"),
      new ApiResponse(description = "Task not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Task output to be saved", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Examples.TaskOutputFormat]))))
  @Produces(Array("application/json"))
  def saveTaskOutput: Route = put {
    caseUser { user =>
      path(Segment) { taskId =>
        entity(as[ValueMap]) { outputParams =>
          askTask(user, taskId, (caseInstanceId, tenantUser) => new SaveTaskOutput(tenantUser, caseInstanceId, taskId, outputParams))
        }
      }
    }
  }

  @Path("/{taskId}/claim")
  @PUT
  @Operation(
    summary = "Claim a task",
    description = "Claim a task such that the current user can perform it",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "taskId", description = "The id of the task to claim", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Task claimed successfully", responseCode = "202"),
      new ApiResponse(description = "Task not found", responseCode = "404"),
    )
  )
  @Produces(Array("application/json"))
  def claimTaskRoute: Route =
    put {
      caseUser { user =>
        path(Segment / "claim") {
          taskId => askTask(user, taskId, (caseInstanceId, tenantUser) => new ClaimTask(tenantUser, caseInstanceId, taskId))
        }
      }
    }

  @Path("/{taskId}/revoke")
  @PUT
  @Operation(
    summary = "Revoke a task",
    description = "Revoke a task such that another user can claim it",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "taskId", description = "The id of the task to revoke", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Task revoked successfully", responseCode = "202"),
      new ApiResponse(description = "Task not found", responseCode = "404"),
    )
  )
  @Produces(Array("application/json"))
  def revokeTaskRoute: Route =
    put {
      caseUser { user =>
        path(Segment / "revoke") {
          taskId => askTask(user, taskId, (caseInstanceId, tenantUser) => new RevokeTask(tenantUser, caseInstanceId, taskId))
        }
      }
    }

  @Path("/{taskId}/assign")
  @PUT
  @Operation(
    summary = "Assign a task",
    description = "Assign a task to the specified user",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "taskId", description = "The unique id of the task to assign", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Task assigned successfully", responseCode = "202"),
      new ApiResponse(description = "Task not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Id of the user to which the task must be assigned", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Assignee]))))
  @Produces(Array("application/json"))
  def assignTaskRoute: Route =
    put {
      caseUser { user =>
        path(Segment / "assign") {
          taskId =>
            requestEntityPresent {
              entity(as[Assignee]) { data =>
                askTaskWithAssignee(user, taskId, data.assignee, (caseInstanceId, tenantUser, assignee) => new AssignTask(tenantUser, caseInstanceId, taskId, assignee))
              }
            }
        }
      }
    }

  @Path("/{taskId}/delegate")
  @PUT
  @Operation(
    summary = "Delegate a task",
    description = "Delegate a task to the specified user",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "taskId", description = "The unique id of the task to delegate", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Task delegated successfully", responseCode = "202"),
      new ApiResponse(description = "Task not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Id of the user to which the task must be assigned", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Assignee]))))
  @Produces(Array("application/json"))
  def delegateTaskRoute: Route =
    put {
      caseUser { user =>
        path(Segment / "delegate") {
          taskId =>
            requestEntityPresent {
              entity(as[Assignee]) { data =>
                askTaskWithAssignee(user, taskId, data.assignee, (caseInstanceId, tenantUser, assignee) => new DelegateTask(tenantUser, caseInstanceId, taskId, assignee))
              }
            }
        }
      }
    }

  @Path("/{taskId}/complete")
  @POST
  @Operation(
    summary = "Complete a task",
    description = "Complete a task with the specified output",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "taskId", description = "The id of the task to complete", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Task completed successfully", responseCode = "202"),
      new ApiResponse(description = "Task not found", responseCode = "404"),
      new ApiResponse(description = "Unable to complete the task because the task output is invalid", responseCode = "400"),
    )
  )
  @RequestBody(description = "Output (optional) to complete the task with", required = false, content = Array(new Content(schema = new Schema(implementation = classOf[Examples.TaskOutputFormat]))))
  @Produces(Array("application/json"))
  def completeTaskRoute: Route =
    post {
      caseUser { user =>
        path(Segment / "complete") {
          taskId =>
            requestEntityPresent {
              entity(as[ValueMap]) { taskOutput =>
                askTask(user, taskId, (caseInstanceId, tenantUser) => new CompleteHumanTask(tenantUser, caseInstanceId, taskId, taskOutput))
              }
            } ~ requestEntityEmpty {
              // Complete the task with empty output parameters
              askTask(user, taskId, (caseInstanceId, tenantUser) => new CompleteHumanTask(tenantUser, caseInstanceId, taskId, new ValueMap))
            }
        }
      }
    }
}
