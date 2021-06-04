/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.server.Directives._
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.cafienne.akka.actor.CaseSystem

import javax.ws.rs.{Consumes, DELETE, PUT, Path}
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.UserQueries
import org.cafienne.service.api.tenant.model.TenantAPI
import org.cafienne.tenant.akka.command._

class DeprecatedTenantOwnersRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends TenantRoute {
  override val addToSwaggerRoutes = false

  // POST Method has been replaced with PUT method. Keeping this for compatibility
  override def routes = concat(addTenantOwner, removeTenantOwner, enableTenantUser, disableTenantUser)

  @Path("/{tenant}/owners/{userId}")
  @PUT
  @Operation(
    summary = "Add a tenant owner",
    description = "Add a user to the group of owners of the tenant. Only tenant owners have permission to manage tenant user information",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "userId", description = "Id of user to be added", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "tenant", description = "The tenant to add the owner to", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Owner added successfully"),
      new ApiResponse(responseCode = "400", description = "Owner information is invalid"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Consumes(Array("application/json"))
  def addTenantOwner = put {
    validUser { tenantOwner =>
      path(Segment / "owners" / Segment) { (tenant, userId) =>
        askTenant(tenantOwner, tenant, tenantUser => new UpdateTenantUser(tenantUser, TenantUserInformation(userId, owner = Some(true))))
      }
    }
  }

  @Path("/{tenant}/owners/{userId}")
  @DELETE
  @Operation(
    summary = "Remove a tenant owner",
    description = "Remove the user with the specified id from the group of tenant owners",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "userId", description = "Id of user to be removed", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "tenant", description = "The tenant to remove the owner from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "Owner removed successfully"),
      new ApiResponse(responseCode = "400", description = "Owner information is invalid"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Consumes(Array("application/json"))
  def removeTenantOwner = delete {
    validUser { tenantOwner =>
      path(Segment / "owners" / Segment) { (tenant, userId) =>
        askTenant(tenantOwner, tenant, tenantUser => new UpdateTenantUser(tenantUser, TenantUserInformation(userId, owner = Some(false))))
      }
    }
  }

  @Path("/{tenant}/users/{userId}/disable")
  @PUT
  @Operation(
    summary = "Disable the tenant user",
    description = "Disable the tenant user",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "userId", description = "Id of user to disable", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "tenant", description = "The tenant in which to disable the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant user disabled successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant user information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def disableTenantUser = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "disable") { (tenant, userId) =>
        askTenant(tenantOwner, tenant, tenantUser => new UpdateTenantUser(tenantUser, TenantUserInformation(userId, enabled = Some(false))))
      }
    }
  }

  @Path("/{tenant}/users/{userId}/enable")
  @PUT
  @Operation(
    summary = "Enable the tenant user",
    description = "Enable the tenant user",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "userId", description = "Id of user to enable", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "tenant", description = "The tenant in which to enable the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant user enabled successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant user information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def enableTenantUser = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "enable") { (tenant, userId) =>
        askTenant(tenantOwner, tenant, tenantUser => new UpdateTenantUser(tenantUser, TenantUserInformation(userId, enabled = Some(true))))
      }
    }
  }
}
