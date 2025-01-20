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

package org.cafienne.service.http.consentgroup.route

import org.apache.pekko.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.persistence.querydb.query.{TenantQueriesImpl, UserQueries}
import org.cafienne.service.http.consentgroup.model.ConsentGroupAPI.{ConsentGroupResponseFormat, ConsentGroupUserFormat}
import org.cafienne.system.CaseSystem

import jakarta.ws.rs._

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("consent-group")
class ConsentGroupMembersRoute(override val caseSystem: CaseSystem) extends ConsentGroupRoute {
  override val userQueries: UserQueries = new TenantQueriesImpl

  override def routes: Route = concat(getGroup, getMember)

  @Path("/{groupId}")
  @GET
  @Operation(
    summary = "Get a consent group",
    description = "Get the consent group and it's members. Can only be retrieved by group members",
    tags = Array("consent-group"),
    parameters = Array(
      new Parameter(name = "groupId", description = "The consent group to retrieve", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Consent group information", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[ConsentGroupResponseFormat])))),
      new ApiResponse(description = "Consent group not found", responseCode = "404"),
    )
  )
  @Produces(Array("application/json"))
  def getGroup: Route = get {
    consentGroupUser { user =>
      pathEndOrSingleSlash { // Need to use pathEnd here otherwise getMember route gets swallowed
        runQuery(userQueries.getConsentGroup(user, user.groupId))
      }
    }
  }

  @Path("/{groupId}/members/{userId}")
  @GET
  @Operation(
    summary = "Get a consent group member",
    description = "Get information about the consent group member with the specified user id. Can only be retrieved by group members.",
    tags = Array("consent-group"),
    parameters = Array(
      new Parameter(name = "groupId", description = "The id of the consent group to retrieve the member from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "The user id to read", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Consent group member information", content = Array(new Content(schema = new Schema(implementation = classOf[ConsentGroupUserFormat])))),
      new ApiResponse(responseCode = "404", description = "Member not found"),
    )
  )
  @Produces(Array("application/json"))
  def getMember: Route = get {
    consentGroupUser { user =>
      path("members" / Segment) { userId =>
        runQuery(userQueries.getConsentGroupMember(user, user.groupId, userId))
      }
    }
  }
}
