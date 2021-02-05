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
import org.cafienne.cmmn.akka.command.platform.{CaseUpdate, NewUserInformation, PlatformUpdate, TenantUpdate}

import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.platform.akka.command.{GetUpdateStatus, UpdatePlatformInformation}
import org.cafienne.service.api.projection.query.PlatformQueries
import org.cafienne.service.api.tenant.model.TenantAPI
import org.cafienne.service.api.tenant.route.TenantRoute
import org.cafienne.tenant.akka.command.platform.{DisableTenant, EnableTenant}

import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/platform")
class PlatformRoute(platformQueries: PlatformQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {

  override def routes = concat(createTenant, disableTenant, enableTenant, getUserInformation, updateUserInformation, getWhereUsedInformation, getUpdateStatus)

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
      validOwner { platformOwner =>
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
    validOwner { platformOwner =>
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
    validOwner { platformOwner =>
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
      new ApiResponse(description = "User information update in progress", responseCode = "202"),
      new ApiResponse(description = "User information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "List of new user information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.PlatformUsersUpdateFormat]))))
  @Consumes(Array("application/json"))
  def updateUserInformation = put {
    validOwner { platformOwner =>
      pathPrefix("user") {
        pathEndOrSingleSlash {
          import spray.json.DefaultJsonProtocol._
          import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

          implicit val userFormat = jsonFormat2(TenantAPI.PlatformUserUpdateFormat)
          implicit val listFormat = jsonFormat1(TenantAPI.PlatformUsersUpdateFormat)
          entity(as[TenantAPI.PlatformUsersUpdateFormat]) { list =>
            readLastModifiedHeader() { lastModified =>
              val newUserIds = list.users.map(u => u.newUserId)
              val existingUserIds = list.users.map(u => u.existingUserId)
              logger.warn("Received request to update platform users " + list)
              onComplete(handleSyncedQuery(() => platformQueries.hasExistingUserIds(newUserIds), lastModified)) {
                case Success(value) => value.size match {
                  case 0 => {
                    logger.warn("New user ids are not in use; retrieving where used information across the system for the existing users ids")
                    val startWhereUsedQueries = System.currentTimeMillis()
                    val queries = for {
                      tenantsByUser <- platformQueries.whereUsedInTenants(existingUserIds)
                      casesByUser <- platformQueries.whereUsedInCases(existingUserIds)
                    } yield (tenantsByUser, casesByUser)
                    onComplete(queries) {
                      case Success(value) => {
                        val finishedWhereUsedQueries = System.currentTimeMillis()
                        logger.warn(s"Existing user ids are found in ${value._1.size} tenants and ${value._2.size} cases; query took ${finishedWhereUsedQueries - startWhereUsedQueries} millis")

                        // Create a base list of NewUserInformation from which a selection will be added to each TenantUpdate and CaseUpdate
                        val newUserInfo = list.users.map(user => NewUserInformation(user.existingUserId, user.newUserId))

                        // Convert query results to command objects for inside the engine
                        val tenantsToUpdate = value._1.map(tenantUserInfo => {
                          val tenant = tenantUserInfo._1
                          val tenantUsers = newUserInfo.filter(info => tenantUserInfo._2.contains(info.existingUserId))
                          TenantUpdate(tenant, PlatformUpdate(tenantUsers))
                        })

                        val casesToUpdate = value._2.map(caseUserInfo => {
                          val caseId = caseUserInfo._1._1
                          val tenant = caseUserInfo._1._2
                          val users = caseUserInfo._2
                          val caseUsers = newUserInfo.filter(info => users.contains(info.existingUserId))
                          CaseUpdate(caseId, tenant, PlatformUpdate(caseUsers))
                        })

                        // Make it Java-ish and inform the platform
                        import scala.collection.JavaConverters._
                        val tenants = seqAsJavaList(tenantsToUpdate.toSeq)
                        val cases = seqAsJavaList(casesToUpdate.toSeq)
                        askModelActor(new UpdatePlatformInformation(platformOwner, PlatformUpdate(newUserInfo), tenants, cases))
                      }
                      case Failure(t) => handleFailure(t)
                    }
                  }
                  case _ => {
                    val error = "Cannot apply new user ids; found existing ids: " + value.mkString(", ")
                    logger.warn(error)
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

  @Path("/where-used-info")
  @POST
  @Operation(
    summary = "Get where used information across the platform for a list of users to be updated",
    description = "Get where used information across the platform for a list of users to be updated",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(description = "String message with the usage statistics", responseCode = "200"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "List of new user information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.PlatformUsersUpdateFormat]))))
  @Consumes(Array("application/json"))
  def getWhereUsedInformation = post {
    validOwner { _ =>
      pathPrefix("where-used-info") {
        pathEndOrSingleSlash {
          import spray.json.DefaultJsonProtocol._
          import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

          implicit val userFormat = jsonFormat2(TenantAPI.PlatformUserUpdateFormat)
          implicit val listFormat = jsonFormat1(TenantAPI.PlatformUsersUpdateFormat)
          entity(as[TenantAPI.PlatformUsersUpdateFormat]) { list =>
            val newUserIds = list.users.map(u => u.newUserId)
            val existingUserIds = list.users.map(u => u.existingUserId)
            val startWhereUsedQueries = System.currentTimeMillis()
            val queries = for {
              tenantsByUser <- platformQueries.whereUsedInTenants(existingUserIds)
              casesByUser <- platformQueries.whereUsedInCases(existingUserIds)
              tenantsByNewUser <- platformQueries.whereUsedInTenants(newUserIds)
              casesByNewUser <- platformQueries.whereUsedInCases(newUserIds)
            } yield (tenantsByUser, casesByUser, tenantsByNewUser, casesByNewUser)
            onComplete(queries) {
              case Success(value) => {
                val finishedWhereUsedQueries = System.currentTimeMillis()
                val queryTimingMsg = s"Query took ${finishedWhereUsedQueries - startWhereUsedQueries} millis"
                val existingMsg = s"Existing user ids are found in ${value._1.size} tenants and ${value._2.size} cases; query took ${finishedWhereUsedQueries - startWhereUsedQueries} millis"
                val newMsg = s"New user ids are found in ${value._3.size} tenants and ${value._4.size} cases"

                val msg = s"$queryTimingMsg\n$existingMsg\n$newMsg"
                complete(StatusCodes.OK, msg)
              }
              case Failure(t) => handleFailure(t)
            }
          }
        }
      }
    }
  }

  @Path("/update-status")
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
  def getUpdateStatus = get {
    pathPrefix("update-status") {
      pathEndOrSingleSlash {
        validOwner { platformOwner =>
          askModelActor(new GetUpdateStatus(platformOwner))
        }
      }
    }
  }
}
