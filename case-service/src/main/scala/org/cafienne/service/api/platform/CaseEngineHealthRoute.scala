/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.platform

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import javax.ws.rs._
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.infrastructure.akka.http.route.CaseServiceRoute

@Api(tags = Array("platform"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/platform")
class CaseEngineHealthRoute() extends CaseServiceRoute {

  override def routes = {
      health ~
      version
  }

  @Path("/health")
  @GET
  @Operation(
    summary = "Get platform health information",
    description = "Retrieves the health status information of the Case Engine",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Platform health is ok", content = Array(new Content(schema = new Schema(implementation = classOf[Object])))),
      new ApiResponse(responseCode = "503", description = "Platform health is not ok"),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def health = get {
    pathPrefix("health") {
      pathEndOrSingleSlash {
        val value = HttpEntity(ContentTypes.`application/json`, CaseSystem.health.status)
        complete(StatusCodes.OK, value)
        }
      }
    }

  @Path("/version")
  @GET
  @Operation(
    summary = "Get platform version",
    description = "Retrieves the version information of the platform",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Version information", content = Array(new Content(schema = new Schema(implementation = classOf[Object])))),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def version = get {
    path("version") {
      val value = HttpEntity(ContentTypes.`application/json`, CaseSystem.version.toString)
      complete(StatusCodes.OK, value)
    }
  }
}
