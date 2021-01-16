/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.platform

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{onComplete, _}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.PlatformQueries
import org.cafienne.service.api.tenant.model.TenantAPI
import org.cafienne.service.api.tenant.route.TenantRoute
import org.cafienne.tenant.akka.command.platform.{DisableTenant, EnableTenant}

import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/platform")
class PlatformRoute(platformQueries: PlatformQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {

  override def routes = {
      createTenant ~
      disableTenant ~
      enableTenant ~
      getUserInformation ~
      updateUserInformation
  }

  @Path("/")
  @POST
  @Operation(
    summary = "Register a new tenant",
    description = "Register a new tenant with it's owners",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(description = "Tenant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "Tenant", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.TenantFormat]))))
  @Consumes(Array("application/json"))
  def createTenant = post {
    pathEndOrSingleSlash {
      validUser { platformOwner =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._

        implicit val userFormat = jsonFormat6(TenantAPI.UserFormat)
        implicit val tenantFormat = jsonFormat3(TenantAPI.BackwardsCompatibleTenantFormat)
        entity(as[TenantAPI.BackwardsCompatibleTenantFormat]) { newTenant =>
          invokeCreateTenant(platformOwner, newTenant)
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
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def disableTenant = put {
    validUser { platformOwner =>
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
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  def enableTenant = put {
    validUser { platformOwner =>
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
      new ApiResponse(responseCode = "200", description = "All user information known within the platform", content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.PlatformUserFormat])))),
      new ApiResponse(responseCode = "400", description = "Invalid request"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def getUserInformation = get {
    pathPrefix("user") {
      pathEndOrSingleSlash {
        validUser { platformUser =>
          completeJsonValue(platformUser.toValue)
        }
      }
    }
  }

  @Path("/user")
  @PUT
  @Operation(
    summary = "Update user information across the platform",
    description = "Update user information across the platform",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(description = "User information update in progress", responseCode = "204"),
      new ApiResponse(description = "User information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "List of new user information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.PlatformUsersUpdateFormat]))))
  @Consumes(Array("application/json"))
  def updateUserInformation = put {
    validUser { platformOwner =>
      pathPrefix("user") {
        pathEndOrSingleSlash {
          import spray.json.DefaultJsonProtocol._
          import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

          implicit val userFormat = jsonFormat2(TenantAPI.PlatformUserUpdateFormat)
          implicit val listFormat = jsonFormat1(TenantAPI.PlatformUsersUpdateFormat)
          entity(as[TenantAPI.PlatformUsersUpdateFormat]) { list =>
            readLastModifiedHeader() { lastModified =>
              println("\n New attempt to update " + list.users.size +" users\n")
              val newUserIds = list.users.map(u => u.newUserId)
              val existingUserIds = list.users.map(u => u.existingUserId)
              onComplete(handleSyncedQuery(() => platformQueries.hasExistingUserIds(newUserIds), lastModified)) {
                case Success(value) => value.size match {
                  case 0 => {
                    val now = System.currentTimeMillis()
                    val queries = for {
                      tenantsByUser <- platformQueries.whereUsedInTenants(existingUserIds)
                      casesByUser <- platformQueries.whereUsedInCases(existingUserIds)
                    } yield (tenantsByUser, casesByUser)
                    onComplete(queries) {
                      case Success(value) => {
                        val done = System.currentTimeMillis()
                        println("Found existing tenants: " + value._1)
                        println("\nAfter " + (done - now) + "Found existing cases: " + value._2)
                        complete(StatusCodes.BadRequest, "Not yet implemented, dear onwer " + platformOwner.userId)
                      }
                      case Failure(t) => handleFailure(t)
                    }
                  }
                  case _ => {
                    val error = "Cannot apply new user ids; found existing ids: " + value.mkString(", ")
                    println(error)
                    complete(StatusCodes.BadRequest, error)
                  }
                }
                case Failure(t) => handleFailure(t)
              }
            }
          }
        }
      }
    }
  }
}
