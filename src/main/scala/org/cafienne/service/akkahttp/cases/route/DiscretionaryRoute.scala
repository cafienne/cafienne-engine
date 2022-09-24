/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route

import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.cmmn.actorapi.command.plan.{AddDiscretionaryItem, GetDiscretionaryItems}
import org.cafienne.cmmn.actorapi.response.CaseResponseModels
import org.cafienne.service.akkahttp.cases.model.CasePlanAPI._
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class DiscretionaryRoute(override val caseSystem: CaseSystem) extends CasesRoute {
  override def routes: Route = concat(retrieveDiscretionaryItem, planDiscretionaryItem)

  @Path("/{caseInstanceId}/discretionaryitems")
  @GET
  @Operation(
    summary = "Get a list of currently applicable discretionary items",
    description = "Returns a list of discretionary items with respect to the current state of the case instance and the user roles",
    tags = Array("discretionary items"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
    ),
    responses = Array(
      new ApiResponse(description = "Items found and returned", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseModels.DiscretionaryItemsList])))),
      new ApiResponse(description = "No items found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def retrieveDiscretionaryItem: Route = get {
    caseUser { user =>
      path(Segment / "discretionaryitems") { caseInstanceId =>
        askCase(user, caseInstanceId, tenantUser => new GetDiscretionaryItems(tenantUser, caseInstanceId))
      }
    }
  }

  @Path("/{caseInstanceId}/discretionaryitems/plan")
  @POST
  @Operation(
    summary = "Plan a discretionary item",
    description = "Plan a discretionary item for the provided case instance",
    tags = Array("discretionary items"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Item is planned", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseModels.PlannedDiscretionaryItem])))),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Item to be planned", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseModels.PlannedDiscretionaryItem]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def planDiscretionaryItem: Route = post {
    caseUser { user =>
      path(Segment / "discretionaryitems" / "plan") { caseInstanceId =>
        entity(as[PlanDiscretionaryItem]) { payload =>
          askCase(user, caseInstanceId, tenantUser => new AddDiscretionaryItem(tenantUser, caseInstanceId, payload.name, payload.definitionId, payload.parentId, payload.planItemId.orNull))
        }
      }
    }
  }
}
