/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.server.Directives.{path, _}
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.cmmn.akka.command.team._
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.service.api.cases.{CaseQueries, CaseReader}

@Api(tags = Array("case team"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseTeamRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute with CaseReader {

  override def routes = setCaseTeam ~ addCaseTeamMember ~ deleteCaseTeamMember

  @Path("/{caseInstanceId}/caseteam")
  @POST
  @Operation(
    summary = "Sets a new case team",
    description = "Sets a new case team for a case instance",
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
  @RequestBody(description = "Case team in JSON format", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseTeam]))))
  @Consumes(Array("application/json"))
  def setCaseTeam = post {
    validUser { user =>
      path(Segment / "caseteam") { caseInstanceId =>
        entity(as[CaseTeam]) { caseTeam =>
          askCase(user, caseInstanceId, user => new SetCaseTeam(user, caseInstanceId, caseTeam))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/caseteam")
  @PUT
  @Operation(
    summary = "Add or update a case team member",
    description = "Add a new case team member or change the roles of an existing member",
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
  @RequestBody(description = "Case Team Member", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseTeamMember]))))
  @Consumes(Array("application/json"))
  def addCaseTeamMember = put {
    validUser { user =>
      path(Segment / "caseteam") { caseInstanceId =>
        entity(as[CaseTeamMember]) { caseTeamMember =>
          askCase(user, caseInstanceId, user => new PutTeamMember(user, caseInstanceId, caseTeamMember))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/caseteam/{userId}")
  @DELETE
  @Operation(
    summary = "Delete a case team member",
    description = "Delete a case team member",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "Unique id of the case team member", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to delete a case team member has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @Consumes(Array("application/json"))
  def deleteCaseTeamMember = delete {
    validUser { user =>
      path(Segment / "caseteam" / Segment) { (caseInstanceId, userId) =>
        askCase(user, caseInstanceId, user => new RemoveTeamMember(user, caseInstanceId, userId))
      }
    }
  }
}
