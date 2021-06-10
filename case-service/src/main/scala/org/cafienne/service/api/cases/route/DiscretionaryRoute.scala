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
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.CaseSystem

import javax.ws.rs._
import org.cafienne.cmmn.actorapi.command.plan.{AddDiscretionaryItem, GetDiscretionaryItems}
import org.cafienne.cmmn.actorapi.command.CaseCommandModels
import org.cafienne.cmmn.actorapi.response.CaseResponseModels
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.service.db.query.CaseQueries

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class DiscretionaryRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CasesRoute {

  override def routes = concat(retrieveDiscretionaryItem, planDiscretionaryItem)

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
  def retrieveDiscretionaryItem = get {
    validUser { platformUser =>
      path(Segment / "discretionaryitems") { caseInstanceId =>
        askCase(platformUser, caseInstanceId, tenantUser => new GetDiscretionaryItems(tenantUser, caseInstanceId))
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
      new ApiResponse(description = "Your request to set a case team has been accepted", responseCode = "201", content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseModels.PlannedDiscretionaryItem])))),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @RequestBody(description = "Item to be planned", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseModels.PlannedDiscretionaryItem]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def planDiscretionaryItem = post {
    validUser { platformUser =>
      path(Segment / "discretionaryitems" / "plan") { caseInstanceId =>
        entity(as[CaseCommandModels.PlanDiscretionaryItem]) { payload =>
          askCase(platformUser, caseInstanceId, tenantUser => new AddDiscretionaryItem(tenantUser, caseInstanceId, payload.name, payload.definitionId, payload.parentId, payload.planItemId.orNull))
        }
      }
    }
  }
}
