/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.consentgroup.route

import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.consentgroup.actorapi.command._
import org.cafienne.querydb.query.{TenantQueriesImpl, UserQueries}
import org.cafienne.service.akkahttp.consentgroup.model.ConsentGroupAPI._
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("consent-group")
class ConsentGroupOwnersRoute(override val caseSystem: CaseSystem) extends ConsentGroupRoute {
  override val userQueries: UserQueries = new TenantQueriesImpl

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
  @RequestBody(description = "Group to replace", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[ConsentGroupFormat]))))
  @Consumes(Array("application/json"))  def replaceGroup: Route = post {
    consentGroupUser { groupOwner =>
        entity(as[ConsentGroupFormat]) { newGroup =>
          askModelActor(new ReplaceConsentGroup(groupOwner, newGroup.asGroup("")))
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
      new Parameter(name = "group", description = "The consent group in which to add/update the member", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
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
      new Parameter(name = "tenant", description = "Tenant to which the consent group belongs", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "group", description = "The consent group in which to add/update the user", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
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
