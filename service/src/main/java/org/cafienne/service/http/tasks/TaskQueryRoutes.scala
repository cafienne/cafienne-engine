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

package org.cafienne.service.http.tasks

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.infrastructure.lastmodified.Headers
import org.cafienne.persistence.querydb.query.cmmn.TaskCount
import org.cafienne.persistence.querydb.query.cmmn.filter.TaskFilter
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.http.tasks.TaskAPIFormat.TaskResponseFormat

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/tasks")
class TaskQueryRoutes(override val httpService: CaseEngineHttpServer) extends TaskRoute {
  override def routes: Route = concat(getAllTasks, getCaseTasks, getTaskCount, getTask)

  @GET
  @Operation(
    summary = "Get a list of tasks",
    description = "Get a list of tasks filtered by the query parameters",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Specific tenant to get tasks", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "caseName", description = "Filter tasks of cases with this name", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "taskName", description = "Filter tasks with this name", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "taskState", description = "Filter tasks in this state", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String], allowableValues = Array("Assigned", "Unassigned", "Delegated", "Completed", "Suspended", "Terminated"))),
      new Parameter(name = "assignee", description = "Filter tasks with this assignee", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "owner", description = "Owner of the task", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "dueOn", description = "Due date of the task. Provide the date in 'yyyy-mm-dd' format", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "dueBefore", description = "Provide a date in 'yyyy-mm-dd' format", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "dueAfter", description = "Provide a date in 'yyyy-mm-dd' format", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "offset", description = "Skip first offset number of tasks", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0")),
      new Parameter(name = "numberOfResults", description = "Maximum number of tasks to retrieve", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100")),
      new Parameter(name = "sortBy", description = "Field to sort on", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String], allowableValues = Array("taskstate", "assignee", "owner", "duedate", "createdby", "modifiedby", "lastmodified"))),
      new Parameter(name = "sortOrder", description = "Sort direction", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "timeZone", description = "Time zone offset.Provide the time zone offset in the format '+01:00' ", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Tasks found", responseCode = "200", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[TaskResponseFormat]))))),
      new ApiResponse(description = "No tasks found based on query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getAllTasks: Route = get {
    caseUser { user =>
      pathEndOrSingleSlash {
        parameters("tenant".?, "identifiers".?, "caseName".?, "taskName".?, "taskState".?, "assignee".?, "owner".?, "dueOn".?, "dueBefore".?, "dueAfter".?, "sortBy".?, "sortOrder".?, "offset".?(0), "numberOfResults".?(100)) {
          (tenant, identifiers, caseName, taskName, taskState, assignee, owner, dueOn, dueBefore, dueAfter, sortBy, sortOrder, offset, numberOfResults) =>
            optionalHeaderValueByName("timeZone") { timeZone =>
              val area = Area(offset, numberOfResults)
              val sort = Sort.withDefault(sortBy, sortOrder, "lastModified")
              val taskFilter = TaskFilter(tenant = tenant, identifiers = identifiers, caseName = caseName, taskName = taskName, taskState = taskState, assignee = assignee, owner = owner, dueOn = dueOn, dueBefore = dueBefore, dueAfter = dueAfter, timeZone = timeZone)
              runListQuery(taskQueries.getAllTasks(user, taskFilter, area, sort))
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
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Only get tasks after events of this timestamp have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Case Tasks found", responseCode = "200", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[TaskResponseFormat]))))),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @Produces(Array("application/json"))
  def getCaseTasks: Route = get {
    caseUser { user =>
      path("case" / Segment) {
        caseInstanceId => runListQuery(taskQueries.getCaseTasks(caseInstanceId, user))
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
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String], example = "timestamp;caseInstanceId"), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Task found and returned", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[TaskResponseFormat])))),
      new ApiResponse(description = "Task not found", responseCode = "404"),
      new ApiResponse(description = "Some processing error occurred", responseCode = "505")
    )
  )
  @Produces(Array("application/json"))
  def getTask: Route = get {
    caseUser { user =>
      path(Segment) {
        taskId => runQuery(taskQueries.getTask(taskId, user))
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
    )
  )
  @Produces(Array("application/json"))
  def getTaskCount: Route = get {
    caseUser { user =>
      parameters("tenant".?) { tenant =>
        path("user" / "count") {
          runQuery(taskQueries.getCountForUser(user, tenant))
        }
      }
    }
  }

  @Path("/case-name/{caseName}")
  @GET
  @Operation(
    summary = "Get tasks from cases with the specified case name",
    description = "Get tasks from cases with the specified case name",
    tags = Array("tasks"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optional tenant to get tasks from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "caseName", description = "The name of cases to get the tasks for", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Only get tasks after events of this timestamp have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Tasks found", responseCode = "200", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[TaskResponseFormat]))))),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Some processing error occurred", responseCode = "505")
    )
  )
  @Produces(Array("application/json"))
  def getCaseDefinitionTasks: Route = get {
    caseUser { user =>
      path("case-name" / Segment) { caseName =>
        parameters("tenant".?) {
          optionalTenant => runListQuery(taskQueries.getTasksWithCaseName(caseName, optionalTenant, user))
        }
      }
    }
  }
}
