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
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.querydb.query.{TenantQueriesImpl, UserQueries}
import org.cafienne.service.akkahttp.consentgroup.model.ConsentGroupAPI.ConsentGroupUserFormat
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
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
      new ApiResponse(description = "Consent group information", responseCode = "200"),
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
      new Parameter(name = "group", description = "The consent group to retrieve the member from", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "The user id to read", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Consent group member information", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[ConsentGroupUserFormat]))))),
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
