/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.server.Directives.{path, _}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.akka.actor.CaseSystem

import javax.ws.rs._
import org.cafienne.cmmn.actorapi.command.team._
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.service.api.Headers
import org.cafienne.service.api.model.{BackwardCompatibleTeamFormat, BackwardCompatibleTeamMemberFormat, Examples}
import org.cafienne.service.db.query.CaseQueries

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseTeamRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CasesRoute {

  override def routes = concat(getCaseTeam, setCaseTeam, putCaseTeamMember, deleteCaseTeamMember)

  @Path("/{caseInstanceId}/caseteam")
  @GET
  @Operation(
    summary = "Get a case team",
    description = "Get the case team of a case instance",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false)
    ),
    responses = Array(
      new ApiResponse(description = "The case team", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[Examples.CaseTeamResponseFormat])))),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @Produces(Array("application/json"))
  def getCaseTeam = get {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("caseteam") {
        runQuery(caseQueries.getCaseTeam(caseInstanceId, platformUser))
      }
    }
  }

  @Path("/{caseInstanceId}/caseteam")
  @POST
  @Operation(
    summary = "Replace the case team",
    description = "Removes existing cae team, and replaces it with the new team. The new team must have an owner. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to set a case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @RequestBody(description = "Case team in JSON format", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Examples.StartCaseTeamFormat]))))
  @Consumes(Array("application/json"))
  def setCaseTeam = post {
    caseInstanceSubRoute { (platformUser, caseInstanceId) => {
      path("caseteam") {
        entity(as[BackwardCompatibleTeamFormat]) { oldFormat =>
          val caseTeam = teamConverter(oldFormat)
          askCaseWithValidMembers(platformUser, caseTeam.members, caseInstanceId, tenantUser => new SetCaseTeam(tenantUser, caseInstanceId, caseTeam))
        }
      }
    }
    }
  }

  @Path("/{caseInstanceId}/caseteam")
  @PUT
  @Operation(
    summary = "Add or update a case team member",
    description = "Add a new member to the case team or change roles or ownership rights of an existing member. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to update a case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @RequestBody(description = "Case Team Member", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Examples.PutCaseTeamMemberFormat]))))
  @Consumes(Array("application/json"))
  def putCaseTeamMember = put {
    caseInstanceSubRoute { (platformUser, caseInstanceId) => {
      path("caseteam") {
        entity(as[BackwardCompatibleTeamMemberFormat]) { oldFormat =>
          val caseTeamMember = memberConverter(oldFormat)
          askCaseWithValidMembers(platformUser, Seq(caseTeamMember), caseInstanceId, tenantUser => new PutTeamMember(tenantUser, caseInstanceId, caseTeamMember))
        }
      }
    }
    }
  }

  @Path("/{caseInstanceId}/caseteam/{memberId}?type={memberType}")
  @DELETE
  @Operation(
    summary = "Remove a member from the case team",
    description = "Remove a member from the case team. Can be a member of type user as well as role. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId",
        description = "Unique id of the case instance",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true),
      new Parameter(name = "memberId",
        description = "Id of the case team member to remove",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true),
      new Parameter(name = "type",
        description = "Type of member (either 'user' or 'role'). If omitted both 'user' is taken",
        in = ParameterIn.QUERY,
        schema = new Schema(implementation = classOf[String], allowableValues = Array("user", "role")),
        required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to delete a case team member has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @Consumes(Array("application/json"))
  def deleteCaseTeamMember = delete {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("caseteam" / Segment) { memberId =>
        parameters('type ?) { memberType =>
          askCase(platformUser, caseInstanceId, tenantUser => new RemoveTeamMember(tenantUser, caseInstanceId, MemberKey(memberId, memberType.getOrElse("user"))))
        }
      }
    }
  }
}
