/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.json.Value
import org.cafienne.service.akkahttp.Headers
import org.cafienne.service.akkahttp.debug.ModelEventsReader
import org.cafienne.system.CaseSystem

import javax.ws.rs._
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
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
          case Success(value) => completeJsonValue(Value.convert(value.map(_.event.rawJson())))
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
          case Success(value) => completeJsonValue(Value.convert(value.map(v => v.toValue)))
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
          case Success(value) => completeJsonValue(value.toValue)
          case Failure(t) => handleFailure(t)
        }
      }
    }
  }
}
