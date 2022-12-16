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

package org.cafienne.service.akkahttp.storage

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.service.akkahttp.tenant.route.TenantRoute
import org.cafienne.storage.StorageUser
import org.cafienne.storage.actormodel.{ActorMetadata, ActorType}
import org.cafienne.system.CaseSystem

import javax.ws.rs.{DELETE, Path, Produces}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/storage")
class TenantStorageRoute(override val caseSystem: CaseSystem) extends TenantRoute with StorageRoute {
  override def routes: Route = concat(deleteTenant)

  @Path("/tenant/{tenant}")
  @DELETE
  @Operation(
    summary = "Remove the tenant from the system",
    description = "Delete all tenant data from the event journal and the query database",
    tags = Array("storage"),
    parameters = Array(
      new Parameter(
        name = "tenant",
        description = "Unique id of the tenant",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true
      )
    ),
    responses = Array(
      new ApiResponse(description = "Tenant removal initiated", responseCode = "202"),
      new ApiResponse(description = "Tenant not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def deleteTenant: Route = delete {
    pathPrefix("tenant") {
      tenantUser { user =>
        if (!user.isOwner) {
          complete(StatusCodes.Unauthorized, "Only tenant owners can perform this operation")
        } else {
          initiateDataRemoval(ActorMetadata(user = StorageUser(user.id), actorType = ActorType.Tenant, tenant = user.tenant, actorId = user.tenant))
        }
      }
    }
  }
}
