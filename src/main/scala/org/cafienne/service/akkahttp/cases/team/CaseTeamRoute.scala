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

package org.cafienne.service.akkahttp.cases.team

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.cmmn.actorapi.command.team._
import org.cafienne.cmmn.actorapi.command.team.removemember._
import org.cafienne.cmmn.actorapi.command.team.setmember.{SetCaseTeamGroup, SetCaseTeamTenantRole, SetCaseTeamUser}
import org.cafienne.infrastructure.akkahttp.route.CaseTeamValidator
import org.cafienne.service.akkahttp.Headers
import org.cafienne.service.akkahttp.cases.CasesRoute
import CaseTeamAPI._
import org.cafienne.system.CaseSystem

import jakarta.ws.rs._
import scala.util.{Failure, Success}

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/cases")
class CaseTeamRoute(override val caseSystem: CaseSystem) extends CasesRoute with CaseTeamValidator {
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
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("caseteam") {
        runQuery(caseQueries.getCaseTeam(caseInstanceId, user))
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
    caseInstanceSubRoute { (user, caseInstanceId) => {
      path("caseteam") {
        entity(as[Compatible.TeamFormat]) { input =>
          val teamInput = input.asTeam
          authorizeCaseAccess(user, caseInstanceId, member => validateTeam(teamInput, member.tenant, team => askModelActor(new SetCaseTeam(member, caseInstanceId, team))))
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
    caseInstanceSubRoute { (user, caseInstanceId) => {
      path("caseteam" / "users") {
        entity(as[CaseTeamUserFormat]) { input =>
          authorizeCaseAccess(user, caseInstanceId, {
            member => {
              val newTeamMember = input.asCaseTeamUser
              onComplete(getUserOrigin(newTeamMember, member.tenant)) {
                case Success(enrichedUser: CaseTeamUser) => askModelActor(new SetCaseTeamUser(member, caseInstanceId, enrichedUser))
                case Failure(t: Throwable) => complete(StatusCodes.NotFound, t.getLocalizedMessage)
              }
            }
          })
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
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("caseteam" / "users" / Segment) { userId =>
        askCase(user, caseInstanceId, caseMember => new RemoveCaseTeamUser(caseMember, caseInstanceId, userId))
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
    caseInstanceSubRoute { (user, caseInstanceId) => {
      path("caseteam" / "groups") {
        entity(as[GroupFormat]) { input =>
          onComplete(validateConsentGroups(Seq(input.asGroup))) {
            case Success(groups: Seq[CaseTeamGroup]) => askCase(user, caseInstanceId, caseMember => new SetCaseTeamGroup(caseMember, caseInstanceId, groups.head))
            case Failure(t: Throwable) => complete(StatusCodes.NotFound, t.getLocalizedMessage)
          }
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
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("caseteam" / "groups" / Segment) { groupId =>
        askCase(user, caseInstanceId, caseMember => new RemoveCaseTeamGroup(caseMember, caseInstanceId, groupId))
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
    caseInstanceSubRoute { (user, caseInstanceId) => {
      path("caseteam" / "tenant-roles") {
        entity(as[TenantRoleFormat]) { input =>
          askCase(user, caseInstanceId, caseMember => new SetCaseTeamTenantRole(caseMember, caseInstanceId, input.asTenantRole))
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
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("caseteam" / "tenant-roles" / Segment) { tenantRoleName =>
        askCase(user, caseInstanceId, caseMember => new RemoveCaseTeamTenantRole(caseMember, caseInstanceId, tenantRoleName))
      }
    }
  }
}
