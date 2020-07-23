/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.service.api.debug

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import io.swagger.annotations.Api
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs.{GET, Path, Produces}
import org.cafienne.akka.actor.serialization.json.ValueList
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.route.AuthenticatedRoute

import scala.util.{Failure, Success}

@Api(tags = Array("case"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/debug")
class DebugRoute()(override implicit val userCache: IdentityProvider, implicit val system: ActorSystem) extends AuthenticatedRoute {

  val caseEventReader = new ModelEventsReader()

  override def routes =
    pathPrefix("debug") {
      getEvents
    }

  @Path("/{caseInstanceId}")
  @GET
  @Operation(
    summary = "Get the list of events in a case",
    description = "Returns the list of events in a case instance",
    tags = Array("debug"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]))
    ),
    responses = Array(
      new ApiResponse ( description = "Case found and returned", responseCode = "200"),
      new ApiResponse ( description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getEvents = get {
    path(Segment) { caseInstanceId =>
      optionalUser { platformUser =>
        onComplete(caseEventReader.getEvents(platformUser, caseInstanceId)) {
          case Success(value) => completeJsonValue(value)
          case Failure(err) => complete(StatusCodes.NotFound, err)
        }
      }
    }

  }
}

