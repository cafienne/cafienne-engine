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
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.cmmn.actorapi.command.plan.{AddDiscretionaryItem, GetDiscretionaryItems}
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.http.cases.CasesRoute
import org.cafienne.service.http.cases.plan.DiscretionaryAPIFormat._

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/cases")
class DiscretionaryRoute(override val httpService: CaseEngineHttpServer) extends CasesRoute {
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
      new ApiResponse(description = "Items found and returned", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[DiscretionaryItemsList])))),
      new ApiResponse(description = "No items found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def retrieveDiscretionaryItem: Route = get {
    caseUser { user =>
      path(Segment / "discretionaryitems") { caseInstanceId =>
        askCase(user, caseInstanceId, caseMember => new GetDiscretionaryItems(caseMember, caseInstanceId, caseMember.rootCaseId))
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
      new ApiResponse(description = "Item is planned", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[PlannedDiscretionaryItem])))),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Item to be planned", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[PlannedDiscretionaryItem]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def planDiscretionaryItem: Route = post {
    caseUser { user =>
      path(Segment / "discretionaryitems" / "plan") { caseInstanceId =>
        entity(as[PlanDiscretionaryItem]) { payload =>
          askCase(user, caseInstanceId, caseMember => new AddDiscretionaryItem(caseMember, caseInstanceId, caseMember.rootCaseId, payload.name, payload.definitionId, payload.parentId, payload.planItemId.orNull))
        }
      }
    }
  }
}
