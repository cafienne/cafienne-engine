/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.board.route

import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.board.actorapi.command.flow.{BoardFlowCommand, StartFlow}
import org.cafienne.json.ValueMap
import org.cafienne.service.akkahttp.board.model.BoardAPI.{FlowStartedFormat, StartFlowFormat}
import org.cafienne.system.CaseSystem
import org.cafienne.util.Guid

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/board")
class BoardFlowsRoute(override val caseSystem: CaseSystem) extends BoardRoute {
  override def routes: Route = concat(startFlow)

  @Path("{boardId}/flow")
  @POST
  @Operation(
    summary = "Start a flow in the board",
    description = "Start a flow in the board",
    tags = Array("board"),
    responses = Array(
      new ApiResponse(responseCode = "202", description = "Flow Started", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[FlowStartedFormat]))))),
    )
  )
  @RequestBody(description = "Board to create or update", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[StartFlowFormat]))))
  @Produces(Array("application/json"))
  def startFlow: Route = post {
    boardUser { user =>
      path("flow") {
        pathEndOrSingleSlash {
          entity(as[StartFlowFormat]) { flow =>
            askFlow(new StartFlow(user, flow.id.getOrElse(new Guid().toString), flow.subject, flow.data.getOrElse(new ValueMap)))
          }
        }
      }
    }
  }

  def askFlow(command: BoardFlowCommand): Route = askBoard(command)
}
