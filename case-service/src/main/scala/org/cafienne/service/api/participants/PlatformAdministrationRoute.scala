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
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.participants.model.TenantAPI
import org.cafienne.tenant.akka.command.platform.{CreateTenant, DisableTenant, EnableTenant}

import scala.collection.JavaConverters._

@Api(value = "registration", tags = Array("registration"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/registration")
class PlatformAdministrationRoute
  (messageRouter: ActorRef)
  (implicit val system: ActorSystem, implicit val actorRefFactory: ActorRefFactory, override implicit val userCache: IdentityProvider)
  extends TenantRoute {

  override def tenantRegion = messageRouter

  override def routes = {
    createTenant ~
    disableTenant ~
    enableTenant
  }

  @Path("/")
  @POST
  @Operation(
    summary = "Register a new tenant",
    description = "Register a new tenant with it's owners",
    tags = Array("registration"),
    responses = Array(
      new ApiResponse(description = "Tenant registered successfully", responseCode = "204"),
      new ApiResponse(description = "Tenant information is invalid", responseCode = "400"),
      new ApiResponse(description = "Not able to perform the action", responseCode = "500")
    )
  )
  @RequestBody(description = "Tenant", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantAPI.Tenant]))))
  @Consumes(Array("application/json"))
  def createTenant = post {
    pathEndOrSingleSlash {
      validUser { platformOwner =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._

        implicit val userFormat = jsonFormat4(TenantAPI.User)
        implicit val tenantFormat = jsonFormat2(TenantAPI.Tenant)
        entity(as[TenantAPI.Tenant]) { newTenant =>
          val newTenantName = newTenant.name
          val owners = newTenant.owners.map(owner => TenantUser(owner.userId, owner.roles.toSeq, newTenantName, owner.name.getOrElse(""), owner.email.getOrElse(""), true))
          askTenant(new CreateTenant(platformOwner, newTenantName, newTenantName, owners.asJava))
        }
      }
    }
  }

  @Path("/{tenant}/disable")
  @PUT
  @Operation(
    summary = "Disable a tenant",
    description = "Disabling a tenant can only be done by platform owners",
    tags = Array("registration"),
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
        askTenant(new DisableTenant(platformOwner, tenant.name))
      }
    }
  }

  @Path("/{tenant}/enable")
  @PUT
  @Operation(
    summary = "Enable a tenant",
    description = "Enabling a tenant can only be done by platform owners",
    tags = Array("registration"),
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
      path(Segment / "disable") { tenant =>
        askTenant(new EnableTenant(platformOwner, tenant.name))
      }
    }
  }
}
