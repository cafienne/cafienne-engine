/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.service.http.platform

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.infrastructure.route.CaseServiceRoute
import org.cafienne.system.health.HealthMonitor

@Path("/")
class CaseEngineHealthRoute(override val httpService: CaseEngineHttpServer) extends CaseServiceRoute {


  // For now, directly in the main, and not as child of PlatformRoutes;
  //  Otherwise, routes are not available when case system is not healthy (because platform routes are AuthenticatedRoute)
  override def routes = concat(health, version, status)

  registerAPIRoute(this)

  @Path("/status")
  @GET
  @Operation(
    summary = "Get platform health information as http status code",
    description = "Retrieves the health status information of the Case Engine",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Platform health is ok"),
      new ApiResponse(responseCode = "503", description = "Platform health is not ok")
    )
  )
  def status = get {
    pathPrefix("status") {
      pathEndOrSingleSlash {
        if (HealthMonitor.ok()) {
          complete(StatusCodes.OK)
        } else {
          complete(StatusCodes.ServiceUnavailable)
        }
      }
    }
  }

  @Path("/health")
  @GET
  @Operation(
    summary = "Get platform health information",
    description = "Retrieves the health status information of the Case Engine",
    tags = Array("platform"),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Platform health report", content = Array(new Content(schema = new Schema(implementation = classOf[Object])))),
      new ApiResponse(responseCode = "500", description = "Not able to perform the action")
    )
  )
  @Produces(Array("application/json"))
  def health = get {
    pathPrefix("health") {
      pathEndOrSingleSlash {
        completeJson(HealthMonitor.report)
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
      completeJson(caseSystem.version.json)
    }
  }
}
