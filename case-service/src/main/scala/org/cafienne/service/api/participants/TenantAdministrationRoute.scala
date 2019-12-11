/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.participants

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem}
import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.tenant.akka.command.{AddTenantOwner, GetTenantOwners, RemoveTenantOwner}

@Api(value = "registration", tags = Array("registration"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/registration")
class TenantAdministrationRoute
  (messageRouter: ActorRef)
  (implicit val system: ActorSystem, implicit val actorRefFactory: ActorRefFactory, override implicit val userCache: IdentityProvider)
  extends TenantRoute {

  override def tenantRegion = messageRouter

  override def routes = {
    addTenantOwner ~
      removeTenantOwner ~
      getTenantOwners
  }

  @Path("/{tenant}/owners/{userId}")
  @PUT
  @Operation(
    summary = "Add a tenant owner",
    description = "Add a user to the group of owners of the tenant. Only tenant owners have permission to manage tenant user information",
    tags = Array("registration"),
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
        val user = tenantOwner.getTenantUser(tenant)
        askTenant(new AddTenantOwner(user, tenant, userId))
      }
    }
  }

  @Path("/{tenant}/owners/{userId}")
  @DELETE
  @Operation(
    summary = "Remove a tenant owner",
    description = "Remove the user with the specified id from the group of tenant owners",
    tags = Array("registration"),
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
        val user = tenantOwner.getTenantUser(tenant)
        askTenant(new RemoveTenantOwner(user, tenant, userId))
      }
    }
  }

  @Path("/{tenant}/owners")
  @GET
  @Operation(
    summary = "Get tenant owners",
    description = "Retrieves the list of tenant owners",
    tags = Array("registration"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to retrieve owners from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "List of user ids of that are owner of the tenant", content = Array(new Content(schema = new Schema(implementation = classOf[Set[String]])))),
      new ApiResponse(responseCode = "400", description = "Invalid request"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def getTenantOwners = get {
    validUser { tenantOwner =>
      path(Segment / "owners") { tenant =>
        if (tenantOwner.isPlatformOwner) {
//          println("Cannot go there as platform owner")
        }
        askTenant(new GetTenantOwners(tenantOwner.getTenantUser(tenant), tenant))
      }
    }
  }
}
