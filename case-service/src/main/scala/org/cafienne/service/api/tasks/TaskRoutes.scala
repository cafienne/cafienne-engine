/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tasks

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.annotations._
import javax.ws.rs.{GET, POST, PUT, Path, Produces}
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.cmmn.akka.command.CaseCommandModels
import org.cafienne.cmmn.instance.casefile.{ValueList, ValueMap}
import org.cafienne.humantask.akka.command.{AssignTask, ClaimTask, CompleteHumanTask, DelegateTask, HumanTaskCommand, RevokeTask, SaveTaskOutput, ValidateTaskOutput}
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.infrastructure.akka.http.route.CommandRoute
import org.cafienne.service.api
import org.cafienne.service.api.Sort
import org.cafienne.service.api.model.Examples
import org.cafienne.service.api.projection.{CaseSearchFailure, TaskSearchFailure}

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

@Api(tags = Array("tasks"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tasks")
class TaskRoutes(taskQueries: TaskQueries)(override implicit val userCache: IdentityProvider) extends CommandRoute with TaskReader {

  override def apiClasses(): Seq[Class[_]] = {
    Seq(classOf[TaskRoutes])
  }

  override def routes = pathPrefix("tasks") {
    getTasksRoute ~
      getCaseTasks ~
      getTask ~
      getCurrentUserAssignedTasks ~
      validateTaskOutput ~
      saveTaskOutput ~
      claimTaskRoute ~
      revokeTaskRoute ~
      assignTaskRoute ~
      delegateTaskRoute ~
      completeTaskRoute
  }

  @GET
  @Operation(
    summary = "Get a list of tasks",
    description = "Get a list of tasks filtered by the query parameters",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Specific tenant to get tasks", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "caseDefinition", description = "Provide the case definition name", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "assignee", description = "Provide the username for whom the task is assigned", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "taskState", description = "Provide the state of the task", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String], allowableValues = Array("Assigned", "Unassigned", "Delegated", "Completed", "Suspended", "Terminated"))),
      new Parameter(name = "owner", description = "Owner of the task", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "dueOn", description = "Due date of the task.Provide the date in 'yyyy-mm-dd' format", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "dueBefore", description = "Provide a date in 'yyyy-mm-dd' format", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "dueAfter", description = "Provide a date in 'yyyy-mm-dd' format", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "offset", description = "Number of tasks starting from result", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0")),
      new Parameter(name = "numberOfResults", description = "Number of tasks", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100")),
      new Parameter(name = "sortBy", description = "Field to sort on", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String], allowableValues = Array("taskstate", "assignee", "owner", "duedate", "createdby", "modifiedby", "lastmodified"))),
      new Parameter(name = "sortOrder", description = "Sort direction", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "timeZone", description = "Time zone offset.Provide the time zone offset in the format '+01:00' ", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Tasks found", responseCode = "200"),
      new ApiResponse(description = "No tasks found based on query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getTasksRoute = get {
    validUser { user =>
      pathEndOrSingleSlash {
        parameters('tenant ?, 'caseDefinition ?, 'taskState ?, 'assignee ?, 'owner ?, 'dueOn ?, 'dueBefore ?, 'dueAfter ?, 'sortBy ?, 'sortOrder ?, 'offset ? 0, 'numberOfResults ? 100) {
          (tenant, caseDefinition, taskState, assignee, owner, dueOn, dueBefore, dueAfter, sortBy, sortOrder, offset, numberOfResults) =>
            optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
              optionalHeaderValueByName("timeZone") { timeZone =>
                onComplete(handleSyncedQuery(() => taskQueries.getAllTasks(tenant, caseDefinition, taskState, assignee, owner, dueOn, dueBefore, dueAfter,
                  sortBy.map(Sort(_, sortOrder)), offset, numberOfResults, user, timeZone), caseLastModified)) {
                  case Success(values) =>
                    val tasks = new ValueList
                    values.foreach(v => tasks.add(v.toValueMap))
                    complete(StatusCodes.OK, tasks)
                  case Failure(err) => complete(StatusCodes.InternalServerError, err)
                }
              }
            }
        }
      }
    }
  }

  @Path("/case-type/{type}")
  @GET
  @Operation(
    summary = "Get all tasks for a certain type of case",
    description = "Get all tasks for a certain type of case",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optional tenant to get tasks from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "type", description = "The type of case to get the tasks for", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Only get tasks after events of this timestamp have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Tasks found and returned", responseCode = "200"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Some processing error occurred", responseCode = "505")
    )
  )
  @Produces(Array("application/json"))
  def getCaseDefinitionTasks = get {
    validUser { user =>
      path("case-type" / Segment) { caseType =>
        parameters('tenant ?) { optionalTenant =>
          optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
            onComplete(handleSyncedQuery(() => taskQueries.getCaseTypeTasks(caseType, optionalTenant, user), caseLastModified)) {
              case Success(value) =>
                val tasks = new ValueList
                value.foreach(v => tasks.add(v.toValueMap))
                complete(StatusCodes.OK, tasks)
                complete(StatusCodes.OK, tasks)
              case Failure(err) =>
                err match {
                  case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
                  case _ => throw err
                }
            }
          }
        }
      }
    }
  }

  @Path("/case/{caseInstanceId}")
  @GET
  @Operation(
    summary = "Get all tasks of a case instance",
    description = "Get all tasks of a case instance",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "The id of the case to get the tasks from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Only get tasks after events of this timestamp have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Tasks found and returned", responseCode = "200"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Some processing error occurred", responseCode = "505")
    )
  )
  @Produces(Array("application/json"))
  def getCaseTasks = get {
    validUser { user =>
      path("case" / Segment) { caseInstanceId =>
        optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
          onComplete(handleSyncedQuery(() => taskQueries.getCaseTasks(caseInstanceId, user), caseLastModified)) {
            case Success(value) =>
              val tasks = new ValueList
              value.foreach(v => tasks.add(v.toValueMap))
              complete(StatusCodes.OK, tasks)
            case Failure(err) =>
              logger.error("Could not find the task, but got an error " + err.getLocalizedMessage, err)
              err match {
                case c: CaseSearchFailure => complete(StatusCodes.NotFound, c.getLocalizedMessage)
                case _ => throw err
              }
          }
        }
      }
    }
  }

  @Path("/{taskId}")
  @GET
  @Operation(
    summary = "Get a task",
    description = "Get a task",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "taskId", description = "Id of the task to retrieve", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String], example="timestamp;caseInstanceId"), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Task found and returned", responseCode = "200"),
      new ApiResponse(description = "Task not found", responseCode = "404"),
      new ApiResponse(description = "Some processing error occurred", responseCode = "505")
    )
  )
  @Produces(Array("application/json"))
  def getTask = get {
    validUser { user =>
      path(Segment) { taskId =>
        optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
          onComplete(handleSyncedQuery(() => taskQueries.getTask(taskId, user), caseLastModified)) {
            case Success(value) => complete(StatusCodes.OK, value.toValueMap)
            case Failure(err) =>
              err match {
                case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
                case other => throw other
              }
          }
        }
      }
    }
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
  def getCurrentUserAssignedTasks = get {
    validUser { user =>
      parameters('tenant ?) { tenant =>
      path("user" / "count") {
        onComplete(taskQueries.getCountForUser(user, tenant)) {
          case Success(value) =>
            import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
            import spray.json.DefaultJsonProtocol._
            implicit val format = jsonFormat2(TaskCount)
            complete(StatusCodes.OK, value)
          case Failure(ex) => complete(StatusCodes.InternalServerError)
        }
      }}
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
    validUser { user =>
      path(Segment) { taskId =>
        entity(as[ValueMap]) {
          outputParams => askTask(user, taskId, (caseInstanceId, user) => new ValidateTaskOutput(user, caseInstanceId, taskId, outputParams))
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
    validUser { user =>
      path(Segment) { taskId =>
        entity(as[ValueMap]) { outputParams =>
          askTask(user, taskId, (caseInstanceId, user) => new SaveTaskOutput(user, caseInstanceId, taskId, outputParams))
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
      validUser { user =>
        path(Segment / "claim") {
          taskId => askTask(user, taskId, (caseInstanceId, user) => new ClaimTask(user, caseInstanceId, taskId))
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
      validUser { user =>
        path(Segment / "revoke") {
          taskId => askTask(user, taskId, (caseInstanceId, user) => new RevokeTask(user, caseInstanceId, taskId))
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
      validUser { user =>
        path(Segment / "assign") {
          taskId =>
            requestEntityPresent {
              import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
              import spray.json.DefaultJsonProtocol._
              implicit val format = jsonFormat1(CaseCommandModels.Assignee)

              entity(as[CaseCommandModels.Assignee]) { data =>
                askTask(user, taskId, (caseInstanceId, user) => new AssignTask(user, caseInstanceId, taskId, data.assignee))
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
      validUser { user =>
        path(Segment / "delegate") {
          taskId =>
            requestEntityPresent {
              import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
              import spray.json.DefaultJsonProtocol._
              implicit val format = jsonFormat1(CaseCommandModels.Assignee)

              entity(as[CaseCommandModels.Assignee]) { data =>
                askTask(user, taskId, (caseInstanceId, user) => new DelegateTask(user, caseInstanceId, taskId, data.assignee))
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
      validUser { user =>
        path(Segment / "complete") {
          taskId =>
            requestEntityPresent {
              entity(as[ValueMap]) { taskOutput =>
                askTask(user, taskId, (caseInstanceId, user) => new CompleteHumanTask(user, caseInstanceId, taskId, taskOutput))
              }
            } ~ requestEntityEmpty {
              // Complete the task with empty output parameters
              askTask(user, taskId, (caseInstanceId, user) => new CompleteHumanTask(user, caseInstanceId, taskId, new ValueMap))
            }
        }
      }
    }

  def askTask(platformUser: PlatformUser, taskId: String, createTaskCommand: CreateTaskCommand): Route = {
    val retrieveCaseIdAndTenant = taskQueries.authorizeTaskAccess(taskId, platformUser)

    onComplete(retrieveCaseIdAndTenant) {
      case Success(retrieval) => {
        retrieval match {
          case Some(caseInstanceId) => askModelActor(createTaskCommand.apply(caseInstanceId._1, platformUser.getTenantUser(caseInstanceId._2)))
          case None => complete(StatusCodes.NotFound, "A task with id " + taskId + " cannot be found in the system")
        }
      }
      case Failure(error) => {
        error match {
          case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
          case _ => throw error
        }
      }
    }
  }

  trait CreateTaskCommand {
    def apply(caseInstanceId: String, user: TenantUser): HumanTaskCommand
  }

}
