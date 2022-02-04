/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.platform

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.service.api.tenant.model.TenantAPI._
import org.cafienne.service.api.tenant.route.TenantRoute
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.command.platform.{CreateTenant, DisableTenant, EnableTenant}

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/platform")
class PlatformRoute()(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends TenantRoute {

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
      validPlatformOwner { platformOwner =>
        entity(as[TenantFormat]) { newTenant =>
          import scala.jdk.CollectionConverters._
          val newTenantName = newTenant.name
          val users = newTenant.getTenantUsers.asJava
          askPlatform(new CreateTenant(platformOwner, newTenantName, newTenantName, users))
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
    validPlatformOwner { platformOwner =>
      path(Segment / "disable") { tenant =>
        askPlatform(new DisableTenant(platformOwner, tenant.name))
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
    validPlatformOwner { platformOwner =>
      path(Segment / "enable") { tenant =>
        askPlatform(new EnableTenant(platformOwner, tenant.name))
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
          completeJsonValue(platformUser.toValue)
        }
      }
    }
  }
}
