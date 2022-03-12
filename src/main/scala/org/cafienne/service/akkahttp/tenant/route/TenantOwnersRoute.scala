/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.tenant.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.consentgroup.actorapi.command.CreateConsentGroup
import org.cafienne.service.akkahttp.consentgroup.model.ConsentGroupAPI.ConsentGroupFormat
import org.cafienne.service.akkahttp.tenant.model.TenantAPI._
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.command._

import javax.ws.rs._
import scala.jdk.CollectionConverters._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tenant")
class TenantOwnersRoute(override val caseSystem: CaseSystem) extends TenantRoute {

  override def routes: Route = concat(getTenantOwners, setUser, putUser, removeUser, createConsentGroup, setTenant, putTenant, getDisabledUserAccounts)

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
    tenantUser { tenantOwner =>
      path("owners") {
        askTenant(new GetTenantOwners(tenantOwner, tenantOwner.tenant))
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
    tenantUser { tenantOwner =>
      entity(as[ReplaceTenantFormat]) { newTenantInformation =>
        // Map users from external format to TenantUser case class and convert to java List
        val users = newTenantInformation.users.map(user => user.asTenantUser(tenantOwner.tenant))
        askTenant(new ReplaceTenant(tenantOwner, tenantOwner.tenant, users.asJava))
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
    tenantUser { tenantOwner =>
      path("users") {
        entity(as[UserFormat]) { newUser =>
          askTenant(new SetTenantUser(tenantOwner, tenantOwner.tenant, newUser.asTenantUser(tenantOwner.tenant)))
        }
      }
    }
  }

  @Path("/{tenant}/users/{userId}")
  @DELETE
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
    tenantUser { tenantOwner =>
      path("users" / Segment) { userId =>
        askTenant(new RemoveTenantUser(tenantOwner, tenantOwner.tenant, userId))
      }
    }
  }

  @Path("/{tenant}/consent-groups")
  @POST
  @Operation(
    summary = "Create a consent group",
    description = "Register a new consent group in a tenant; the group must have members, and at least one member must be owner.",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to retrieve accounts from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Consent group updated successfully", responseCode = "204"),
      new ApiResponse(responseCode = "404", description = "Consent group not found"),
    )
  )
  @RequestBody(description = "Group to create", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[ConsentGroupFormat]))))
  @Consumes(Array("application/json"))
  def createConsentGroup: Route = post {
    validUser { platformUser =>
      path(Segment / "consent-groups") { tenant =>
        entity(as[ConsentGroupFormat]) { newGroup =>
          val tenantOwner = platformUser.getTenantUser(tenant)
          if (!tenantOwner.isOwner) {
            complete(StatusCodes.Unauthorized, "Only tenant owners can create consent groups")
          } else {
            askModelActor(new CreateConsentGroup(tenantOwner, newGroup.asGroup(tenant)))
          }
        }
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
    tenantUser { tenantOwner =>
      path("disabled-accounts") {
        runListQuery(userQueries.getDisabledTenantUserAccounts(tenantOwner))
      }
    }
  }
}
