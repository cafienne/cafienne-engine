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
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.serialization.json.ValueMap
import org.cafienne.cmmn.akka.command.StartCase
import org.cafienne.cmmn.akka.command.response.CaseStartedResponse
import org.cafienne.cmmn.akka.command.team.CaseTeamMember
import org.cafienne.cmmn.definition.InvalidDefinitionException
import org.cafienne.cmmn.repository.MissingDefinitionException
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.service.api.anonymous.CaseRequestRoute.AnonymousStartCaseFormat
import org.cafienne.util.Guid

import javax.ws.rs._
import scala.annotation.meta.field
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/request")
class CaseRequestRoute(implicit val userCache: IdentityProvider) extends AnonymousRoute {

  // Reading the definitions executes certain validations immediately
  val configuredCaseDefinitions = CaseSystem.config.api.anonymousConfig.definitions

  override def routes = {
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
      new ApiResponse(description = "Something went wrong", responseCode = "500")
    )
  )
  @RequestBody(description = "case", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[AnonymousStartCaseFormat]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def createCase = post {
    readCaseType { caseType: String =>
      entity(as[AnonymousStartCaseFormat]) { payload =>
        try {
          configuredCaseDefinitions.get(caseType) match {
            case Some(definitionConfig) => {
              val newCaseId = payload.caseInstanceId.getOrElse(new Guid().toString)
              val debugMode = payload.debug.getOrElse(CaseSystem.config.actor.debugEnabled)
              val startCaseCommand = definitionConfig.createStartCaseCommand(newCaseId, payload.inputs, debugMode)
              createCaseWithValidTeam(definitionConfig.tenant, definitionConfig.team.members, startCaseCommand)
            }
            case None => complete(StatusCodes.NotFound, s"Request of type '$caseType' is not found")
          }
        } catch {
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

  def createCaseWithValidTeam(tenant: String, members: Seq[CaseTeamMember], command: StartCase): Route = {
    val userIds = members.filter(member => member.isTenantUser()).map(member => member.key.id)
    onComplete(userCache.getUsers(userIds, tenant)) {
      case Success(tenantUsers) => {
        if (tenantUsers.size != userIds.size) {
          val tenantUserIds = tenantUsers.map(t => t.id)
          val unfoundUsers = userIds.filterNot(userId => tenantUserIds.contains(userId))
          val msg = {
            if (unfoundUsers.size == 1) s"Cannot find an active user '${unfoundUsers(0)}' in tenant '$tenant'"
            else s"The users ${unfoundUsers.map(u => s"'$u'").mkString(", ")} are not active in tenant $tenant"
          }
          fail(msg)
        } else {
          sendCommand(command, classOf[CaseStartedResponse], (response: CaseStartedResponse) => {
            writeLastModifiedHeader(response) {
              complete(StatusCodes.OK, s"""{\n  "caseInstanceId": "${response.getActorId}"\n}""")
            }
          })
        }
      }
      case Failure(t: Throwable) => fail(t)
    }
  }
}


final object CaseRequestRoute {

  @Schema(description = "Input parameters example json")
  case class InputParametersFormat(input1: String, input2: Object, input3: List[String])

  @Schema(description = "Start the execution of a new case")
  case class AnonymousStartCaseFormat(
                                       @(Schema@field)(
                                         description = "Input parameters that will be passed to the started case",
                                         required = false,
                                         implementation = classOf[InputParametersFormat])
                                       inputs: ValueMap,
                                       @(Schema@field)(description = "Unique identifier to be used for this case. When there is no identifier given, a UUID will be generated", required = false, example = "Will be generated if omitted or empty")
                                       caseInstanceId: Option[String],
                                       @(Schema@field)(description = "Indicator to start the case in debug mode", required = false, implementation = classOf[Boolean], example = "false")
                                       debug: Option[Boolean])

}

