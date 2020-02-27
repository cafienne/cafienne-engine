/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
import org.cafienne.service.api.tenant.UserQueries
import org.cafienne.service.api.tenant.model.TenantAPI
import org.cafienne.tenant.akka.command.{AddTenantOwner, AddTenantUser, AddTenantUserRoles, DisableTenantUser, EnableTenantUser, GetTenantOwners, RemoveTenantOwner, RemoveTenantUserRole}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

@Api(value = "tenant", tags = Array("tenant"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tenant")
class TenantOwnersRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {

  override def routes = {
    addTenantOwner ~
      removeTenantOwner ~
      getTenantOwners ~
      addTenantUser ~
      addTenantUserRoles ~
      removeTenantUserRole ~
      enableTenantUser ~
      disableTenantUser ~
      getDisabledUserAccounts
  }

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
    tags = Array("tenant"),
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

  @Path("/{tenant}/users")
  @POST
  @Operation(
    summary = "Register the user as a case participant",
    description = "Add a user to the tenant, with the specified roles",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to add the user to", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Participant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Participant information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.User]))))
  @Consumes(Array("application/json"))
  def addTenantUser = post {
    validUser { user =>
      path(Segment / "users") { tenant =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        implicit val format = jsonFormat4(TenantAPI.User)
        entity(as[TenantAPI.User]) {
          newUser =>
            val tenantOwner = user.getTenantUser(tenant)
            val roles = newUser.roles.asJava
            val name = newUser.name.getOrElse("")
            val email = newUser.email.getOrElse("")
            askTenant(new AddTenantUser(tenantOwner, tenant, newUser.userId, roles, name, email))
        }
      }
    }
  }

  @Path("/{tenant}/users/{userId}/disable")
  @PUT
  @Operation(
    summary = "Disable the user as a case participant",
    description = "Disable the user as a case participant",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "userId", description = "Id of user to disable", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "tenant", description = "The tenant in which to disable the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Participant disabled successfully", responseCode = "204"),
      new ApiResponse(description = "Participant information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def disableTenantUser = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "disable") { (tenant, userId) =>
        //        System.err.println("Disabling user " + userId + " in tenant " + tenant)
        val user = tenantOwner.getTenantUser(tenant)
        askTenant(new DisableTenantUser(user, tenant, userId))
      }
    }
  }

  @Path("/{tenant}/users/{userId}/enable")
  @PUT
  @Operation(
    summary = "Enable the user as a case participant",
    description = "Enable the user as a case participant",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "userId", description = "Id of user to enable", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "tenant", description = "The tenant in which to enable the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Participant enabled successfully", responseCode = "204"),
      new ApiResponse(description = "Participant information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def enableTenantUser = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "enable") { (tenant, userId) =>
        //        System.err.println("Enabling user " + userId + " in tenant " + tenant)
        val user = tenantOwner.getTenantUser(tenant)
        askTenant(new EnableTenantUser(user, tenant, userId))
      }
    }
  }

  @Path("/{tenant}/users/{userId}/roles/{role}")
  @PUT
  @Operation(
    summary = "Add a role to the tenant user",
    description = "Add a role to the tenant user",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "userId", description = "Id of user to add roles to", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "tenant", description = "The tenant in which to change the user roles", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "role", description = "The role that has to be removed from the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "User roles updated successfully", responseCode = "204"),
      new ApiResponse(description = "User role information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "Roles", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[List[String]], example = "[\"role1\", \"role2\"]"))))
  @Consumes(Array("application/json"))
  def addTenantUserRoles = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "roles" / Segment) { (tenant, userId, role) =>
        //            System.err.println("New roles for user " + userId + " in tenant " + tenant + ": " + roles)
        val user = tenantOwner.getTenantUser(tenant)
        askTenant(new AddTenantUserRoles(user, tenant, userId, role))
      }
    }
  }

  @Path("/{tenant}/users/{userId}/roles/{role}")
  @DELETE
  @Operation(
    summary = "Remove the role from the tenant user",
    description = "Remove the role with the specified name from the tenant user",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant in which to change the user roles", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "Id of user to remove roles from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "role", description = "The role that has to be removed from the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "User role removed successfully", responseCode = "204"),
      new ApiResponse(description = "User role information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def removeTenantUserRole = delete {
    validUser { user =>
      path(Segment / "users" / Segment / "roles" / Segment) { (tenant, userId, role) =>
        //            System.err.println("Remove role for user " + userId + " in tenant " + tenant + ": " + role)
        val tenantOwner = user.getTenantUser(tenant)
        askTenant(new RemoveTenantUserRole(tenantOwner, tenant, userId, role))
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
      new ApiResponse(responseCode = "204", description = "List of user accounts that have been disabled in the tenant", content = Array(new Content(schema = new Schema(implementation = classOf[Set[TenantUser]])))),
      new ApiResponse(responseCode = "400", description = "Invalid request"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def getDisabledUserAccounts = get {
    validUser { tenantOwner =>
      path(Segment / "disabled-accounts") { tenant =>
        onComplete(userQueries.getDisabledTenantUsers(tenantOwner, tenant)) {
          case Success(users) =>
            complete(StatusCodes.OK, users)
          case Failure(err) =>
            err match {
              case err: SecurityException => complete(StatusCodes.Unauthorized, err.getMessage)
              case _ => complete(StatusCodes.InternalServerError, err)
            }
        }

    }
    }
  }
}
