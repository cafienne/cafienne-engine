/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.server.Directives._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.db.query.UserQueries
import org.cafienne.service.api.tenant.model.TenantAPI
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.command._

import javax.ws.rs._
import scala.jdk.CollectionConverters._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tenant")
class TenantOwnersRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends TenantRoute {

  override def routes =
    concat(getTenantOwners, upsertTenantUser, updateTenant, replaceTenantUser, replaceTenant, addTenantUserRoles, removeTenantUserRole, getDisabledUserAccounts)

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
  def getTenantOwners = get {
    validUser { tenantOwner =>
      path(Segment / "owners") { tenant =>
        askTenant(tenantOwner, tenant, tenantUser => new GetTenantOwners(tenantUser))
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
  @RequestBody(description = "Users to update", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.UpdateTenantFormat]))))
  @Consumes(Array("application/json"))
  def replaceTenant = post {
    validUser { platformUser =>
      path(Segment) { tenant =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        implicit val userFormat = jsonFormat6(TenantAPI.UserFormat)
        implicit val tenantFormat = jsonFormat1(TenantAPI.UpdateTenantFormat)
        entity(as[TenantAPI.UpdateTenantFormat]) { newTenantInformation =>
          // Map users from external format to TenantUser case class and convert to java List
          val users = newTenantInformation.users.map(user => asTenantUser(user, tenant)).asJava
          askTenant(platformUser, tenant, tenantOwner => new ReplaceTenant(tenantOwner, users))
        }
      }
    }
  }

  @Path("/{tenant}")
  @PUT
  @Operation(
    summary = "Bulk update the tenant users",
    description = "Add or replace the existing tenant users. If the user does not yet exist it will be created. The existing user properties and roles are updated if new information is given",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant in which to add/update the users", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant updated successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
    )
  )
  @RequestBody(description = "Users to update", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.UpdateTenantFormat]))))
  @Consumes(Array("application/json"))
  def updateTenant = put {
    validUser { platformUser =>
      path(Segment) { tenant =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        implicit val userFormat = jsonFormat6(TenantAPI.UserFormat)
        implicit val tenantFormat = jsonFormat1(TenantAPI.UpdateTenantFormat)
        entity(as[TenantAPI.UpdateTenantFormat]) { newTenantInformation =>
          // Map users from external format to TenantUser case class and convert to java List
          val users = newTenantInformation.users.map(user => asTenantUser(user, tenant)).asJava
          askTenant(platformUser, tenant, tenantOwner => new UpdateTenant(tenantOwner, users))
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
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.UserFormat]))))
  @Consumes(Array("application/json"))
  def replaceTenantUser = post {
    validUser { platformUser =>
      path(Segment / "users") { tenant =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        implicit val format = jsonFormat6(TenantAPI.UserFormat)
        entity(as[TenantAPI.UserFormat]) { newUser =>
          askTenant(platformUser, tenant, tenantOwner => new ReplaceTenantUser(tenantOwner, asTenantUser(newUser, tenant)))
        }
      }
    }
  }

  @Path("/{tenant}/users")
  @PUT
  @Operation(
    summary = "Add or update a tenant user",
    description = "Creates or updates a tenant user. Only the properties defined in the request entity will be updated in an existing user; for new users sensible defaults are chosen if the properties are not set",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant in which to add/update the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Tenant user registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant user information is invalid", responseCode = "400"),
    )
  )
  @RequestBody(description = "User information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.UserFormat]))))
  @Consumes(Array("application/json"))
  def upsertTenantUser = put {
    validUser { platformUser =>
      path(Segment / "users") { tenant =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        implicit val format = jsonFormat6(TenantAPI.UserFormat)
        entity(as[TenantAPI.UserFormat]) { newUser =>
          askTenant(platformUser, tenant, tenantOwner => new UpsertTenantUser(tenantOwner, asTenantUser(newUser, tenant)))
        }
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
    )
  )
  @RequestBody(description = "Roles", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[List[String]], example = "[\"role1\", \"role2\"]"))))
  @Consumes(Array("application/json"))
  def addTenantUserRoles = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "roles" / Segment) { (tenant, userId, role) =>
        askTenant(tenantOwner, tenant, tenantUser => new AddTenantUserRole(tenantUser, userId, role))
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
    )
  )
  def removeTenantUserRole = delete {
    validUser { platformUser =>
      path(Segment / "users" / Segment / "roles" / Segment) { (tenant, userId, role) =>
        askTenant(platformUser, tenant, tenantUser => new RemoveTenantUserRole(tenantUser, userId, role))
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
  def getDisabledUserAccounts = get {
    validUser { tenantOwner =>
      path(Segment / "disabled-accounts") {
        tenant => runListQuery(userQueries.getDisabledTenantUsers(tenantOwner, tenant))
      }
    }
  }
}
