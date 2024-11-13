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

package com.casefabric.service.http.cases.history

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import com.casefabric.json.Value
import com.casefabric.querydb.lastmodified.Headers
import com.casefabric.service.http.debug.ModelEventsReader
import com.casefabric.system.CaseSystem

import scala.util.{Failure, Success}

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/cases")
class CaseHistoryRoute(override val caseSystem: CaseSystem) extends CaseEventsBaseRoute {
  override def routes: Route = concat(getCaseEvents, getPlanHistory, getPlanItemHistory)

  val modelEventsReader = new ModelEventsReader(caseSystem)

  @Path("/{caseInstanceId}/history/events")
  @GET
  @Operation(
    summary = "Get the plan items of a case",
    description = "Get the plan items of the specified case instance",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Plan items found", responseCode = "200"),
      new ApiResponse(description = "No plan items found based on the query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCaseEvents: Route = get {
    caseEventsSubRoute { caseEvents =>
      path("history" / "events") {
        onComplete(caseEvents.eventList()) {
          case Success(events) => completeJson(Value.convert(events.map(_.event.rawJson())))
          case Failure(err) => complete(StatusCodes.NotFound, err)
        }
      }
    }
  }

  @Path("/{caseInstanceId}/history/planitems")
  @GET
  @Operation(
    summary = "Get the plan items of a case",
    description = "Get the plan items of the specified case instance",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Plan items found", responseCode = "200"),
      new ApiResponse(description = "No plan items found based on the query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanHistory: Route = get {
    caseEventsSubRoute { caseEvents =>
      path("history" / "planitems") {
        onComplete(caseEvents.casePlanHistory()) {
          case Success(items) => completeJson(Value.convert(items.map(_.toValue)))
          case Failure(t) => handleFailure(t)
        }
      }
    }
  }

  @Path("/{caseInstanceId}/history/planitems/{planItemId}")
  @GET
  @Operation(
    summary = "Get history of a plan item in a case by planItemId",
    description = "Get history of a plan item in the specified case instance",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "planItemId", description = "Unique id of the plan item (cannot be the plan item name, must be the id)", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Plan item found", responseCode = "200"),
      new ApiResponse(description = "No plan item found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItemHistory: Route = get {
    caseEventsSubRoute { caseEvents =>
      path("history" / "planitems" / Segment) { planItemId =>
        onComplete(caseEvents.planitemHistory(planItemId)) {
          case Success(value) => completeJson(value)
          case Failure(t) => handleFailure(t)
        }
      }
    }
  }
}
