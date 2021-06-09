/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.service.api.debug

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.TerminateModelActor
import org.cafienne.akka.actor.command.response.{EngineChokedFailure, ModelResponse, SecurityFailure}
import org.cafienne.akka.actor.config.Cafienne
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.route.{AuthenticatedRoute, CommandRoute}
import org.cafienne.service.Main

import javax.ws.rs.{GET, PATCH, Path, Produces}
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/debug")
class DebugRoute()(override implicit val userCache: IdentityProvider, implicit val system: ActorSystem, override implicit val caseSystem: CaseSystem) extends CommandRoute {

  val modelEventsReader = new ModelEventsReader()

  // NOTE: although documented with Swagger, this route is not exposed in the public Swagger documentation!
  //  Reason: avoid too easily using this route in development of applications, as that introduces a potential security issue.
  override def routes =
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
  def getEvents = get {
    path(Segment) { modelId =>
      optionalUser { platformUser =>
        parameters('from ? 0L, 'to ? Long.MaxValue) { (from: Long, to: Long) => {
          onComplete(modelEventsReader.getEvents(platformUser, modelId, from, to)) {
            case Success(value) => completeJsonValue(value)
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
  def forceRecovery = patch {
    path("force-recovery" / Segment) { modelId =>
      validUser { _ =>
        if (! Cafienne.config.developerRouteOpen) {
          complete(StatusCodes.NotFound)
        } else {
          caseSystem.router ! new TerminateModelActor(modelId)
          complete(StatusCodes.OK, s"Forced recovery of $modelId")
        }
      }
    }
  }
}
