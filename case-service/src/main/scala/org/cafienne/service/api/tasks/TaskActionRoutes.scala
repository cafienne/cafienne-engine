/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tasks

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.cmmn.akka.command.CaseCommandModels
import org.cafienne.cmmn.instance.casefile.ValueMap
import org.cafienne.humantask.akka.command._
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.service.api.model.Examples
import org.cafienne.service.api.projection.query.{TaskCount, TaskQueries}

@Api(tags = Array("tasks"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tasks")
class TaskActionRoutes(val taskQueries: TaskQueries)(override implicit val userCache: IdentityProvider) extends TaskRoute {

  override def routes = {
    validateTaskOutput ~
      saveTaskOutput ~
      claimTaskRoute ~
      revokeTaskRoute ~
      assignTaskRoute ~
      delegateTaskRoute ~
      completeTaskRoute
  }

  @Path("/user/count")
  @GET
  @Operation(
    summary = "Get task count",
    description = "Count of assigned tasks for current user",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant in which tasks must be counted", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Count of assigned and other tasks", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[TaskCount])))),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @Produces(Array("application/json"))
  def getTaskCount = get {
    validUser { platformUser =>
      parameters('tenant ?) { tenant =>
        path("user" / "count") {
          runQuery(taskQueries.getCountForUser(platformUser, tenant))
        }
      }
    }
  }

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
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "Task output to be validated", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Examples.OutputParameters]))))
  @Produces(Array("application/json"))
  def validateTaskOutput = post {
    validUser { platformUser =>
      path(Segment) { taskId =>
        entity(as[ValueMap]) {
          outputParams => askTask(platformUser, taskId, (caseInstanceId, tenantUser) => new ValidateTaskOutput(tenantUser, caseInstanceId, taskId, outputParams))
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
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "Task output to be saved", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Examples.OutputParameters]))))
  @Produces(Array("application/json"))
  def saveTaskOutput = put {
    validUser { platformUser =>
      path(Segment) { taskId =>
        entity(as[ValueMap]) { outputParams =>
          askTask(platformUser, taskId, (caseInstanceId, tenantUser) => new SaveTaskOutput(tenantUser, caseInstanceId, taskId, outputParams))
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
      new ApiResponse(description = "Task with the specified id cannot be found", responseCode = "404"),
      new ApiResponse(description = "Unable to claim the task due to an internal failure", responseCode = "500")
    )
  )
  @Produces(Array("application/json"))
  def claimTaskRoute =
    put {
      validUser { platformUser =>
        path(Segment / "claim") {
          taskId => askTask(platformUser, taskId, (caseInstanceId, tenantUser) => new ClaimTask(tenantUser, caseInstanceId, taskId))
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
      new ApiResponse(description = "Task with the specified id cannot be found", responseCode = "404"),
      new ApiResponse(description = "Unable to revoke the task due to an internal failure", responseCode = "500")
    )
  )
  @Produces(Array("application/json"))
  def revokeTaskRoute =
    put {
      validUser { platformUser =>
        path(Segment / "revoke") {
          taskId => askTask(platformUser, taskId, (caseInstanceId, tenantUser) => new RevokeTask(tenantUser, caseInstanceId, taskId))
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
      new ApiResponse(description = "Task with the specified id cannot be found", responseCode = "404"),
      new ApiResponse(description = "Unable to assign the task due to an internal failure", responseCode = "500")
    )
  )
  @RequestBody(description = "Id of the user to which the task must be assigned", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseCommandModels.Assignee]))))
  @Produces(Array("application/json"))
  def assignTaskRoute =
    put {
      validUser { platformUser =>
        path(Segment / "assign") {
          taskId =>
            requestEntityPresent {
              import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
              import spray.json.DefaultJsonProtocol._
              implicit val format = jsonFormat1(CaseCommandModels.Assignee)

              entity(as[CaseCommandModels.Assignee]) { data =>
                askTaskWithMember(platformUser, taskId, data.assignee, (caseInstanceId, tenantUser, assignee) => new AssignTask(tenantUser, caseInstanceId, taskId, assignee))
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
      new ApiResponse(description = "Task with the specified id cannot be found", responseCode = "404"),
      new ApiResponse(description = "Unable to delegate the task due to an internal failure", responseCode = "500")
    )
  )
  @RequestBody(description = "Id of the user to which the task must be assigned", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseCommandModels.Assignee]))))
  @Produces(Array("application/json"))
  def delegateTaskRoute =
    put {
      validUser { platformUser =>
        path(Segment / "delegate") {
          taskId =>
            requestEntityPresent {
              import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
              import spray.json.DefaultJsonProtocol._
              implicit val format = jsonFormat1(CaseCommandModels.Assignee)

              entity(as[CaseCommandModels.Assignee]) { data =>
                askTaskWithMember(platformUser, taskId, data.assignee, (caseInstanceId, tenantUser, assignee) => new DelegateTask(tenantUser, caseInstanceId, taskId, assignee))
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
      new ApiResponse(description = "Task with the specified id cannot be found", responseCode = "404"),
      new ApiResponse(description = "Unable to complete the task because the task output is invalid", responseCode = "400"),
      new ApiResponse(description = "Unable to complete the task due to an internal failure", responseCode = "500")
    )
  )
  @RequestBody(description = "Output (optional) to complete the task with", required = false, content = Array(new Content(schema = new Schema(implementation = classOf[Examples.OutputParameters]))))
  @Produces(Array("application/json"))
  def completeTaskRoute =
    post {
      validUser { platformUser =>
        path(Segment / "complete") {
          taskId =>
            requestEntityPresent {
              entity(as[ValueMap]) { taskOutput =>
                askTask(platformUser, taskId, (caseInstanceId, tenantUser) => new CompleteHumanTask(tenantUser, caseInstanceId, taskId, taskOutput))
              }
            } ~ requestEntityEmpty {
              // Complete the task with empty output parameters
              askTask(platformUser, taskId, (caseInstanceId, tenantUser) => new CompleteHumanTask(tenantUser, caseInstanceId, taskId, new ValueMap))
            }
        }
      }
    }
}
