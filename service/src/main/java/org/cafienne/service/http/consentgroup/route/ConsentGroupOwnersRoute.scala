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

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.consentgroup.actorapi.command._
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.http.consentgroup.model.ConsentGroupAPI._

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("consent-group")
class ConsentGroupOwnersRoute(override val httpService: CaseEngineHttpServer) extends ConsentGroupRoute {
  override def routes: Route = concat(replaceGroup, setGroupMember, removeGroupMember)

  @Path("/{groupId}")
  @POST
  @Operation(
    summary = "Replace the consent group",
    description = "Overwrite the member information of the existing group",
    tags = Array("consent-group"),
    parameters = Array(
      new Parameter(name = "groupId", description = "The group to be replaced", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Consent group updated successfully", responseCode = "204"),
      new ApiResponse(responseCode = "404", description = "Consent group not found"),
    )
  )
  @RequestBody(description = "Group to replace", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[ReplaceConsentGroupFormat]))))
  @Consumes(Array("application/json"))  def replaceGroup: Route = post {
    consentGroupUser { groupOwner =>
        entity(as[ConsentGroupFormat]) { newGroup =>
          askModelActor(new ReplaceConsentGroup(groupOwner, newGroup.asGroup(groupOwner)))
        }
    }
  }

  @Path("/{groupId}/members")
  @POST
  @Operation(
    summary = "Add or replace a member in the consent group",
    description = "Replaces the roles and ownership if the member is already in the consent group, otherwise creates a new member.",
    tags = Array("consent-group"),
    parameters = Array(
      new Parameter(name = "groupId", description = "The consent group in which to add/update the member", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Member registered successfully", responseCode = "204"),
      new ApiResponse(responseCode = "404", description = "Member not found"),
    )
  )
  @RequestBody(description = "Member information", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[ConsentGroupUserFormat]))))
  @Consumes(Array("application/json"))
  def setGroupMember: Route = post {
    consentGroupUser { groupOwner =>
      path("members") {
        entity(as[ConsentGroupUserFormat]) { newMember =>
          askConsentGroup(new SetConsentGroupMember(groupOwner, newMember.asMember))
        }
      }
    }
  }

  @Path("/{groupId}/members/{userId}")
  @DELETE
  @Operation(
    summary = "Remove the member from the consent group",
    description = "Removes the member with the specified user id from the consent group; checks that at least one consent group owner remains",
    tags = Array("consent-group"),
    parameters = Array(
      new Parameter(name = "groupId", description = "The consent group in which to add/update the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "Identifier of the user to remove from the consent group", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Member removed successfully", responseCode = "204"),
      new ApiResponse(description = "Member not found", responseCode = "404"),
    )
  )
  @Consumes(Array("application/json"))
  def removeGroupMember: Route = delete {
    consentGroupUser { groupOwner =>
      path("members" / Segment) { userId =>
        askConsentGroup(new RemoveConsentGroupMember(groupOwner, userId))
      }
    }
  }
}
