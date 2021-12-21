/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.cmmn.actorapi.command.team._
import org.cafienne.cmmn.actorapi.command.team.removemember._
import org.cafienne.cmmn.actorapi.command.team.setmember.{SetCaseTeamGroup, SetCaseTeamTenantRole}
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.Headers
import org.cafienne.service.api.cases.model.CaseTeamAPI._
import org.cafienne.service.db.query.CaseQueries
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseTeamRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CasesRoute {

  override def routes: Route = concat(getCaseTeam, setCaseTeam, setUser, deleteUser, setGroup, deleteGroup, setTenantRole, deleteTenantRole)

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
    )
  )
  @Produces(Array("application/json"))
  def getCaseTeam: Route = get {
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
    description = "Removes the existing case team, and replaces it with the new team. The new team must have an owner. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to set a case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Case team in JSON format", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TeamFormat]))))
  @Consumes(Array("application/json"))
  def setCaseTeam: Route = post {
    caseInstanceSubRoute { (platformUser, caseInstanceId) => {
      path("caseteam") {
        entity(as[Compatible.TeamFormat]) { input =>
          val teamInput = input.asTeam
          authorizeCaseAccess(platformUser, caseInstanceId, member => validateTeam(teamInput, member.tenant, team => askModelActor(new SetCaseTeam(member, caseInstanceId, team))))
        }
      }
    }
    }
  }

  @Path("/{caseInstanceId}/caseteam/users")
  @POST
  @Operation(
    summary = "Add or replace a case team user",
    description = "Add a new or replace an existing user in the case team. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to update the case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Case Team User", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseTeamUserFormat]))))
  @Consumes(Array("application/json"))
  def setUser: Route = post {
    caseInstanceSubRoute { (platformUser, caseInstanceId) => {
      path("caseteam" / "users") {
        entity(as[CaseTeamUserFormat]) { input =>
          putCaseTeamUser(platformUser, caseInstanceId, input.asCaseTeamUser)
        }
      }
    }
    }
  }

  @Path("/{caseInstanceId}/caseteam/users/{userId}")
  @DELETE
  @Operation(
    summary = "Remove a user from the case team",
    description = "Remove a user from the case team. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId",
        description = "Unique id of the case instance",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true),
      new Parameter(name = "userId",
        description = "Id of the user to remove from the case team",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to remove a case team user has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @Consumes(Array("application/json"))
  def deleteUser: Route = delete {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("caseteam" / "users" / Segment) { userId =>
        askCase(platformUser, caseInstanceId, tenantUser => new RemoveCaseTeamUser(tenantUser, caseInstanceId, userId))
      }
    }
  }

  @Path("/{caseInstanceId}/caseteam/groups")
  @POST
  @Operation(
    summary = "Add or replace a case team member of type consent group",
    description = "Adds a new or replaces and existing consent group member of the case team. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to update the case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Case Team Group", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[GroupFormat]))))
  @Consumes(Array("application/json"))
  def setGroup: Route = post {
    caseInstanceSubRoute { (platformUser, caseInstanceId) => {
      path("caseteam" / "groups") {
        entity(as[GroupFormat]) { input =>
          askCase(platformUser, caseInstanceId, user => new SetCaseTeamGroup(user, caseInstanceId, input.asGroup))
        }
      }
    }
    }
  }

  @Path("/{caseInstanceId}/caseteam/groups/{groupId}")
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
      new Parameter(name = "groupId",
        description = "Id of the consent group to remove",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to remove the consent group from the case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @Consumes(Array("application/json"))
  def deleteGroup: Route = delete {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("caseteam" / "groups" / Segment) { groupId =>
        askCase(platformUser, caseInstanceId, tenantUser => new RemoveCaseTeamGroup(tenantUser, caseInstanceId, groupId))
      }
    }
  }

  @Path("/{caseInstanceId}/caseteam/tenant-roles")
  @POST
  @Operation(
    summary = "Add or replace a tenant role in the case team",
    description = "Add a new or replace an existing tenant role in the case team. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to update the case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @RequestBody(description = "Tenant Role", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[TenantRoleFormat]))))
  @Consumes(Array("application/json"))
  def setTenantRole: Route = post {
    caseInstanceSubRoute { (platformUser, caseInstanceId) => {
      path("caseteam" / "tenant-roles") {
        entity(as[TenantRoleFormat]) { input =>
          askCase(platformUser, caseInstanceId, user => new SetCaseTeamTenantRole(user, caseInstanceId, input.asTenantRole))
        }
      }
    }
    }
  }

  @Path("/{caseInstanceId}/caseteam/tenant-roles/{tenantRoleName}")
  @DELETE
  @Operation(
    summary = "Remove a tenant role from the case team",
    description = "Remove a tenant role from the case team. Changes to the team can only be done by case owners.",
    tags = Array("case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId",
        description = "Unique id of the case instance",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true),
      new Parameter(name = "tenantRoleName",
        description = "Name of the tenant role that must be removed from the case team",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to delete a case team member has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @Consumes(Array("application/json"))
  def deleteTenantRole: Route = delete {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("caseteam" / "tenant-roles" / Segment) { tenantRoleName =>
        askCase(platformUser, caseInstanceId, tenantUser => new RemoveCaseTeamTenantRole(tenantUser, caseInstanceId, tenantRoleName))
      }
    }
  }
}
