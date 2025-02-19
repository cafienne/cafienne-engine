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

package org.cafienne.service.http.debug

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs.{GET, PATCH, Path, Produces}
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.actormodel.command.TerminateModelActor
import org.cafienne.infrastructure.cqrs.instance.ModelEventsReader
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.infrastructure.route.CommandRoute

import scala.util.{Failure, Success}

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/debug")
class DebugRoute(override val httpService: CaseEngineHttpServer) extends CommandRoute {

  private val modelEventsReader = new ModelEventsReader(caseSystem)

  // NOTE: although documented with Swagger, this route is not exposed in the public Swagger documentation!
  //  Reason: avoid too easily using this route in development of applications, as that introduces a potential security issue.
  override def routes: Route =
    pathPrefix("debug") {
      concat(getEvents, forceRecovery)
    }

  @Path("/{modelId}")
  @GET
  @Operation(
    summary = "Get a range of events from a model actor (a case, tenant or process task)",
    description = "Returns the list of events in a case, tenant or process",
    tags = Array("debug"),
    parameters = Array(
      new Parameter(name = "modelId", description = "Unique id of the model actor", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "from", description = "Events starting sequence number (defaults to 0)", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Long]), required = false),
      new Parameter(name = "to", description = "Events starting sequence number (defaults to Long.MaxValue)", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Long]), required = false)
    ),
    responses = Array(
      new ApiResponse(description = "Events in a json list", responseCode = "200"),
      new ApiResponse(description = "Model actor not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getEvents: Route = get {
    path(Segment) { modelId =>
      optionalUser { platformUser =>
        parameters("from".?(0L), "to".?(Long.MaxValue)) { (from: Long, to: Long) => {
          onComplete(modelEventsReader.getEvents(platformUser, modelId, from, to)) {
            case Success(value) => completeJson(value)
            case Failure(err) => complete(StatusCodes.NotFound, err)
          }
        }
        }
      }
    }
  }

  @Path("force-recovery/{tenant}/{modelId}")
  @PATCH
  @Operation(
    summary = "Force recovery on a model actor",
    description = "Returns the list of events in a case, tenant or process",
    tags = Array("debug"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Name of the tenant in which the actor lives", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "modelId", description = "Identifier of the actor (eg. case id or tenant name)", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
    ),
    responses = Array(
      new ApiResponse(description = "Events in a json list", responseCode = "200"),
      new ApiResponse(description = "Model actor not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def forceRecovery: Route = patch {
    path("force-recovery" / Segment) { modelId =>
      validUser { user =>
        if (!caseSystem.config.developerRouteOpen) {
          complete(StatusCodes.NotFound)
        } else {
          onComplete(caseSystem.gateway.request(TerminateModelActor(modelId))) {
            case Success(value) => complete(StatusCodes.OK, s"Forced recovery of $modelId")
            case Failure(err) => throw err;
          }
        }
      }
    }
  }
}
