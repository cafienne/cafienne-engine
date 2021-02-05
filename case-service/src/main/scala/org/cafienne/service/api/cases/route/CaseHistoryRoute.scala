/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.server.Directives.{path, _}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api
import org.cafienne.service.api.projection.query.CaseQueries

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseHistoryRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute {

  override def routes = concat(getPlanHistory, getPlanItemHistory)

  @Path("/{caseInstanceId}/history/planitems")
  @GET
  @Operation(
    summary = "Get the plan items of a case",
    description = "Get the plan items of the specified case instance",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "PlanItems found", responseCode = "200"),
      new ApiResponse(description = "No PlanItems found based on the query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanHistory = get {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("history" / "planitems") {
         runListQuery(caseQueries.getCasePlanHistory(caseInstanceId, platformUser))
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
      new Parameter(name = "planItemId", description = "Unique id of the planItem (cannot be the plan item name, must be the id)", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "PlanItem found", responseCode = "200"),
      new ApiResponse(description = "No PlanItem found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItemHistory = get {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("history" / "planitems" / Segment) { planItemId =>
        runQuery(caseQueries.getPlanItemHistory(planItemId, platformUser))
      }
    }
  }
}
