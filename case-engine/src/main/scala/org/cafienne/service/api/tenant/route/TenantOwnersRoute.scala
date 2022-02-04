/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.service.api.tenant.model.TenantAPI._
import org.cafienne.service.db.query.UserQueries
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.command._

import javax.ws.rs._
import scala.jdk.CollectionConverters._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tenant")
class TenantOwnersRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends TenantRoute {

  override def routes: Route =
    concat(getTenantOwners, setUser, putUser, removeUser, setTenant, putTenant, getDisabledUserAccounts)

  @Path("/{tenant}/owners")
  @GET
  @Operation(
    summary = "Get tenant owners",
    description = "Retrieves the list of tenant owners",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to retrieve owners from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "List of user ids of that are owner of the tenant", content = Array(new Content(schema = new Schema(implementation = classOf[Set[String]])))),
      new ApiResponse(responseCode = "404", description = "Tenant not found"),
    )
  )
  @Produces(Array("application/json"))
  def getTenantOwners: Route = get {
    validUser { tenantOwner =>
      path(Segment / "owners") { tenant =>
        askTenant(tenantOwner, tenant, tenantUser => new GetTenantOwners(tenantUser, tenant))
      }
    }
  }

  @Path("/{tenant}")
  @POST
  @Operation(
    summary = "Replace the tenant",
    description = "Replace the existing tenant users. Existing user-accounts not in the new list will disabled. New users in the list will be added, others will have their properties and roles replaced with the new information",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant in which to replace the information", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant replaced successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
    )
  )
  @RequestBody(description = "Users to update", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[ReplaceTenantFormat]))))
  @Consumes(Array("application/json"))
  def setTenant: Route = post {
    replaceTenant
  }

  def putTenant: Route = put {
    replaceTenant // For backwards compatibility temporarily support PUT as well.
  }

  private def replaceTenant: Route = {
    validUser { platformUser =>
      path(Segment) { tenant =>
        entity(as[ReplaceTenantFormat]) { newTenantInformation =>
          // Map users from external format to TenantUser case class and convert to java List
          val users = newTenantInformation.users.map(user => user.asTenantUser(tenant))
          askTenant(platformUser, tenant, tenantOwner => new ReplaceTenant(tenantOwner, tenant, users.asJava))
        }
      }
    }
  }

  @Path("/{tenant}/users")
  @POST
  @Operation(
    summary = "Add or replace a tenant user",
    description = "Replace the properties and roles of a tenant user if it exists, otherwise creates a new user.",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant in which to add/update the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant user registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant user information is invalid", responseCode = "400"),
    )
  )
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[UserFormat]))))
  @Consumes(Array("application/json"))
  def setUser: Route = post { replaceUser }
  // For compatibility continue to support PUT for some time on the same
  def putUser: Route = put { replaceUser }

  def replaceUser: Route = {
    validUser { platformUser =>
      path(Segment / "users") { tenant =>
        entity(as[UserFormat]) { newUser =>
          askTenant(platformUser, tenant, tenantOwner => new SetTenantUser(tenantOwner, tenant, newUser.asTenantUser(tenant)))
        }
      }
    }
  }

  @Path("/{tenant}/users/{userId}")
  @POST
  @Operation(
    summary = "Remove a tenant user",
    description = "Removes the user from the tenant (if it exists and is not the last owner)",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant from which to remove the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "The id of the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant user removed successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant user information is invalid", responseCode = "400"),
    )
  )
  def removeUser: Route = delete {
    validUser { platformUser =>
      path(Segment / "users" / Segment) { (tenant, userId) =>
        askTenant(platformUser, tenant, tenantOwner => new RemoveTenantUser(tenantOwner, tenant, userId))
      }
    }
  }

  @Path("/{tenant}/disabled-accounts")
  @GET
  @Operation(
    summary = "Get a list of disabled user accounts",
    description = "Retrieves the list of disabled user accounts",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to retrieve accounts from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of user accounts that have been disabled in the tenant", content = Array(new Content(schema = new Schema(implementation = classOf[Set[TenantUser]])))),
      new ApiResponse(responseCode = "404", description = "Tenant not found"),
    )
  )
  @Produces(Array("application/json"))
  def getDisabledUserAccounts: Route = get {
    validUser { tenantOwner =>
      path(Segment / "disabled-accounts") {
        tenant => runListQuery(userQueries.getDisabledTenantUsers(tenantOwner, tenant))
      }
    }
  }
}
