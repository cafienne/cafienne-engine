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

package org.cafienne.service.http.cases.plan

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition
import org.cafienne.cmmn.instance.Transition
import org.cafienne.persistence.infrastructure.lastmodified.Headers
import org.cafienne.service.http.cases.CasesRoute
import org.cafienne.service.http.cases.plan.PlanItemAPIFormat.PlanItemResponseFormat
import org.cafienne.system.CaseSystem

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/cases")
class PlanItemRoute(override val caseSystem: CaseSystem) extends CasesRoute {
  override def routes: Route = concat(getPlanItems, getPlanItem, makePlanItemTransition)

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
      new ApiResponse(description = "Plan items found", responseCode = "200", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[PlanItemResponseFormat]))))),
      new ApiResponse(description = "No plan items found based on the query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItems: Route = get {
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("planitems") {
        runListQuery(caseQueries.getPlanItems(caseInstanceId, user))
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
      new Parameter(name = "planItemId", description = "Unique id of the plan item", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Plan item found", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[PlanItemResponseFormat])))),
      new ApiResponse(description = "Plan item not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItem: Route = get {
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("planitems" / Segment) {
        planItemId => runQuery(caseQueries.getPlanItem(planItemId, user))
      }
    }
  }

  @Path("/{caseInstanceId}/planitems/{identifier}/{transition}")
  @POST
  @Operation(
    summary = "Apply a transition on a plan item",
    description = "Applies a transition to all plan items in the case that have the identifier. If it is the planItemId there will be only one instance. If planItemName is given, more instances may be found",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "identifier", description = "Identifier for the plan item; either a plan item id or a plan item name", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "transition", description = "Transition to apply", in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String], allowableValues = Array("complete", "close", "create", "enable", "disable", "exit", "fault", "manualStart", "occur", "parentResume", "parentSuspend", "reactivate", "reenable", "resume", "start", "suspend", "terminate")),
        required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Transition applied successfully", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @Produces(Array("application/json"))
  def makePlanItemTransition: Route = post {
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("planitems" / Segment / Segment) { (planItemId, transitionString) =>
        val transition = Transition.getEnum(transitionString)
        if (transition == null) {
          complete(StatusCodes.BadRequest, "Transition " + transition + " is not valid")
        } else {
          askCase(user, caseInstanceId, caseMember => new MakePlanItemTransition(caseMember, caseInstanceId, planItemId, transition))
        }
      }
    }
  }
}
