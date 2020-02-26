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
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.instance.casefile.ValueList
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.tenant.model._
import org.cafienne.service.api.tenant.UserQueries

import scala.util.{Failure, Success}


@Api(value = "users", tags = Array("tenant"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tenant")
class TenantUsersRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {

  override def routes = {
      getTenantUsers ~
      getTenantUser
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
}
