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

package org.cafienne.service.http.platform

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.PlatformOwner
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.http.tenant.model.TenantAPI._
import org.cafienne.service.infrastructure.route.CommandRoute
import org.cafienne.tenant.actorapi.command.platform.{CreateTenant, DisableTenant, EnableTenant, PlatformTenantCommand}

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/platform")
class PlatformRoute(override val httpService: CaseEngineHttpServer) extends CommandRoute {

  override def routes: Route = concat(createTenant, disableTenant, enableTenant, getUserInformation)

  @Path("/")
  @POST
  @Operation(
    summary = "Register a new tenant",
    description = "Register a new tenant with one or more users and at least one owner",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(description = "Tenant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
    )
  )
  @RequestBody(description = "Tenant", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantFormat]))))
  @Consumes(Array("application/json"))
  def createTenant: Route = post {
    pathEndOrSingleSlash {
      platformOwner { owner =>
        entity(as[TenantFormat]) { newTenant =>
          import scala.jdk.CollectionConverters._
          val newTenantName = newTenant.name
          val users = newTenant.getTenantUsers.asJava
          askPlatform(new CreateTenant(owner, newTenantName, newTenantName, users))
        }
      }
    }
  }

  @Path("/{tenant}/disable")
  @PUT
  @Operation(
    summary = "Disable a tenant",
    description = "Disabling a tenant can only be done by platform owners",
    tags = Array("platform"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to disable", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
    )
  )
  def disableTenant: Route = put {
    platformOwner { owner =>
      path(Segment / "disable") { tenant =>
        askPlatform(new DisableTenant(owner, tenant.name))
      }
    }
  }

  @Path("/{tenant}/enable")
  @PUT
  @Operation(
    summary = "Enable a tenant",
    description = "Enabling a tenant can only be done by platform owners",
    tags = Array("platform"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to enable", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
    )
  )
  def enableTenant: Route = put {
    platformOwner { owner =>
      path(Segment / "enable") { tenant =>
        askPlatform(new EnableTenant(owner, tenant.name))
      }
    }
  }

  @Path("/user")
  @GET
  @Operation(
    summary = "Get user information of current user",
    description = "Retrieves the user information of current user",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "All user information known within the platform", content = Array(new Content(schema = new Schema(implementation = classOf[PlatformUserFormat])))),
      new ApiResponse(responseCode = "401", description = "User is not registered in the case system"),
    )
  )
  @Produces(Array("application/json"))
  def getUserInformation: Route = get {
    pathPrefix("user") {
      pathEndOrSingleSlash {
        validUser { platformUser =>
          completeJson(platformUser)
        }
      }
    }
  }

  def platformOwner(subRoute: PlatformOwner => Route): Route = {
    authenticatedUser { user =>
      if (caseSystem.config.platform.isPlatformOwner(user)) {
        subRoute(PlatformOwner(user))
      } else {
        complete(StatusCodes.Unauthorized, "Only platform owners can access this route")
      }
    }
  }

  def askPlatform(command: PlatformTenantCommand): Route = {
    askModelActor(command)
  }
}
