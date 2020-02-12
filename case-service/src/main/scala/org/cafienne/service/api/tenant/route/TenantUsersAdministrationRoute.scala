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
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.instance.casefile.ValueList
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.tenant.model._
import org.cafienne.service.api.tenant.UserQueries
import org.cafienne.tenant.akka.command._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}


@Api(value = "users", tags = Array("tenant"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tenant")
class TenantUsersAdministrationRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {

  override def routes = {
    addTenantUser ~
      addTenantUserRoles ~
      removeTenantUserRole ~
      enableTenantUser ~
      disableTenantUser ~
      getTenantUsers ~
      getTenantUser ~
      getUserInformation
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

  @Path("/{tenant}/users")
  @GET
  @Operation(
    summary = "Get tenant users",
    description = "Retrieves the list of tenant users",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to retrieve users from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "List of user ids of that are registered in the tenant", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[TenantAPI.TenantUser]))))),
      new ApiResponse(responseCode = "400", description = "Invalid request"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def getTenantUsers = get {
    validUser { platformUser =>
      path(Segment / "users") { tenant =>
        onComplete(userQueries.getTenantUsers(platformUser, tenant)) {
          case Success(users) =>
            implicit val usersMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { users: Seq[TenantUser] =>
              val vList = new ValueList()
              users.foreach(u => vList.add(u.toJson))
              HttpEntity(ContentTypes.`application/json`, vList.toString)
            }
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

  @Path("/{tenant}/users/{userId}")
  @GET
  @Operation(
    summary = "Get a tenant user",
    description = "Gets information about the tenant user with the specified id",
    tags = Array("tenant"),
    parameters = Array(
      new Parameter(name = "tenant", description = "The tenant to retrieve users from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "The user id to read", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "204", description = "List of user ids of that are registered in the tenant", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[TenantAPI.TenantUser]))))),
      new ApiResponse(responseCode = "400", description = "Invalid request"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def getTenantUser = get {
    validUser { platformUser =>
      path(Segment / "users" / Segment) { (tenant, userId) =>
        platformUser.shouldBelongTo(tenant)

        onComplete(userQueries.getPlatformUser(userId)) {
          case Success(requestedUser) =>
            val tenantUserInformation = requestedUser.getTenantUser(tenant)
            implicit val tenantUserMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { user: TenantUser =>
              HttpEntity(ContentTypes.`application/json`, user.toJson.toString)
            }

            complete(StatusCodes.OK, tenantUserInformation)
          case Failure(err) =>
            err match {
              case err: SecurityException => complete(StatusCodes.Unauthorized, err.getMessage)
              case _ => complete(StatusCodes.InternalServerError, err)
            }
        }
      }
    }
  }

  @Path("/user-information")
  @GET
  @Operation(
    summary = "Get user information of current user",
    description = "Retrieves the user information of current user",
    tags = Array("tenant"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "All user information known within the platform", content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.PlatformUser])))),
      new ApiResponse(responseCode = "400", description = "Invalid request"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def getUserInformation = get {
    pathPrefix("user-information") {
      pathEndOrSingleSlash {
        validUser { user =>
          val value = HttpEntity(ContentTypes.`application/json`, user.toJSON)
          complete(StatusCodes.OK, value)
        }
      }
    }
  }
}
