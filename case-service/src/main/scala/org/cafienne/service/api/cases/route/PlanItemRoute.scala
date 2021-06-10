/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{path, _}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.CaseSystem

import javax.ws.rs._
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition
import org.cafienne.cmmn.instance.Transition
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.Headers
import org.cafienne.service.db.query.CaseQueries

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class PlanItemRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CasesRoute {

  override def routes = concat(getPlanItems, getPlanItem, makePlanItemTransition)

  @Path("/{caseInstanceId}/planitems")
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
      new ApiResponse(description = "PlanItems found", responseCode = "200"),
      new ApiResponse(description = "No PlanItems found based on the query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItems = get {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("planitems") {
        runQuery(caseQueries.getPlanItems(caseInstanceId, platformUser))
      }
    }
  }

  @Path("/{caseInstanceId}/planitems/{planItemId}")
  @GET
  @Operation(
    summary = "Get a plan item of a case",
    description = "Get a plan item of the specified case instance",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "planItemId", description = "Unique id of the planItem", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Plan item found", responseCode = "200"),
      new ApiResponse(description = "Plan item not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItem = get {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("planitems" / Segment) {
        planItemId => runQuery(caseQueries.getPlanItem(planItemId, platformUser))
      }
    }
  }

  @Path("/{caseInstanceId}/planitems/{identifier}/{transition}")
  @POST
  @Operation(
    summary = "Apply a transition on a planItem",
    description = "Applies a transition to all planItems in the case that have the identifier. If it is the planItemId there will be only one instance. If planItemName is given, more instances may be found",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "identifier", description = "Identifier for the planItem; either a plan item id or a plan item name", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "transition", description = "Transition to apply", in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String], allowableValues = Array("complete", "close", "create", "enable", "disable", "exit", "fault", "manualStart", "occur", "parentResume", "parentSuspend", "reactivate", "reenable", "resume", "start", "suspend", "terminate")),
        required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Transition applied successfully", responseCode = "202"),
      new ApiResponse(description = "Unable to apply transition", responseCode = "500")
    )
  )
  @Produces(Array("application/json"))
  def makePlanItemTransition = post {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("planitems" / Segment / Segment) { (planItemId, transitionString) =>
        val transition = Transition.getEnum(transitionString)
        if (transition == null) {
          complete(StatusCodes.BadRequest, "Transition " + transition + " is not valid")
        } else {
          askCase(platformUser, caseInstanceId, tenantUser => new MakePlanItemTransition(tenantUser, caseInstanceId, planItemId, transition))
        }
      }
    }
  }
}
