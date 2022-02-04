/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.anonymous

import _root_.akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.cmmn.actorapi.command.StartCase
import org.cafienne.cmmn.actorapi.response.CaseStartedResponse
import org.cafienne.cmmn.definition.InvalidDefinitionException
import org.cafienne.cmmn.repository.MissingDefinitionException
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.akka.http.route.CaseTeamValidator
import org.cafienne.infrastructure.config.api.AnonymousCaseDefinition
import org.cafienne.service.api.anonymous.model.AnonymousAPI._
import org.cafienne.service.db.query.exception.SearchFailure
import org.cafienne.system.CaseSystem
import org.cafienne.util.Guid

import javax.ws.rs._
import scala.concurrent.ExecutionContext

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/request")
class CaseRequestRoute(implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends AnonymousRoute with CaseTeamValidator {

  // Reading the definitions executes certain validations immediately
  val configuredCaseDefinitions: Map[String, AnonymousCaseDefinition] = Cafienne.config.api.anonymousConfig.definitions
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  override def routes: Route = {
    createCase
  }

  @Path("/case/{case_type}")
  @POST
  @Operation(
    summary = "Request a case instance",
    description = "Returns the caseInstanceId of the started case",
    tags = Array("request"),
    responses = Array(
      new ApiResponse(description = "Case is created and started", responseCode = "201"),
      new ApiResponse(description = "Case definition not available", responseCode = "400"),
    )
  )
  @RequestBody(description = "case", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[AnonymousStartCaseFormat]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def createCase: Route = post {
    readCaseType { caseType: String =>
      entity(as[AnonymousStartCaseFormat]) { payload =>
        try {
          configuredCaseDefinitions.get(caseType) match {
            case Some(definitionConfig) => {
              val newCaseId = payload.caseInstanceId.getOrElse(new Guid().toString)
                validateTenantAndTeam(definitionConfig.team, definitionConfig.tenant, team => {
                  val debugMode = payload.debug.getOrElse(Cafienne.config.actor.debugEnabled)
                  val command = new StartCase(definitionConfig.tenant, definitionConfig.user, newCaseId, definitionConfig.definition, payload.inputs, team, debugMode)
                  sendCommand(command, classOf[CaseStartedResponse], (response: CaseStartedResponse) => {
                    writeLastModifiedHeader(response) {
                      complete(StatusCodes.OK, s"""{\n  "caseInstanceId": "${response.getActorId}"\n}""")
                    }
                  })
                })
            }
            case None => complete(StatusCodes.NotFound, s"Request of type '$caseType' is not found")
          }
        } catch {
          case e: SearchFailure => fail(e)
          case e: MissingDefinitionException => fail(e.getMessage)
          case e: InvalidDefinitionException => fail(e.getMessage)
        }
      }
    }
  }

  private def readCaseType(subRoute: String => Route): Route = {
    // Either we have a '/request/case' or '/request/case/' or '/request/case/{case-type}'
    concat(path("case") { pathEndOrSingleSlash { subRoute("") } },
      path("case" / Remaining) { rawPath => subRoute(rawPath) })
  }
}
