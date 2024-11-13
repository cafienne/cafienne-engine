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

package com.casefabric.service.http.anonymous

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import com.casefabric.cmmn.actorapi.command.StartCase
import com.casefabric.cmmn.actorapi.response.CaseStartedResponse
import com.casefabric.cmmn.definition.InvalidDefinitionException
import com.casefabric.cmmn.repository.MissingDefinitionException
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.infrastructure.config.api.AnonymousCaseDefinition
import com.casefabric.querydb.lastmodified.Headers
import com.casefabric.querydb.query.exception.SearchFailure
import com.casefabric.service.http.anonymous.model.AnonymousAPI._
import com.casefabric.system.CaseSystem
import com.casefabric.util.Guid

import jakarta.ws.rs._
import com.casefabric.service.infrastructure.route.{CaseTeamValidator, LastModifiedDirectives}
import scala.concurrent.ExecutionContext

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/request")
class CaseRequestRoute(override val caseSystem: CaseSystem) extends AnonymousRoute with CaseTeamValidator with LastModifiedDirectives {

  // Reading the definitions executes certain validations immediately
  val configuredCaseDefinitions: Map[String, AnonymousCaseDefinition] = CaseFabric.config.api.anonymousConfig.definitions
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
      new ApiResponse(description = "Case is created and started", responseCode = "201", content = Array(new Content(schema = new Schema(implementation = classOf[AnonymousStartCaseResponseFormat])))),
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
                  val debugMode = payload.debug.getOrElse(CaseFabric.config.actor.debugEnabled)
                  val command = new StartCase(definitionConfig.tenant, definitionConfig.user, newCaseId, definitionConfig.definition, payload.inputs, team, debugMode)
                  sendCommand(command, classOf[CaseStartedResponse], (response: CaseStartedResponse) => {
                    writeLastModifiedHeader(response, Headers.CASE_LAST_MODIFIED) {
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
