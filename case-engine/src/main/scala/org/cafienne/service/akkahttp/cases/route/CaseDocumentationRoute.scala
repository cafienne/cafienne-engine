/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route

import akka.http.scaladsl.server.Directives.{path, _}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.service.akkahttp.Headers
import org.cafienne.service.db.query.CaseQueries
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseDocumentationRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CasesRoute {

  override def routes = concat(getPlanItemDocumentation, getCaseFileDocumentation)

  @Path("/{caseInstanceId}/documentation/planitems/{planItemId}")
  @GET
  @Operation(
    summary = "Get the documentation information from the plan item's definition",
    description = "Get the documentation information from the plan item's definition",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "planItemId", description = "Unique id of the planItem", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Plan item documentation found", responseCode = "200"),
      new ApiResponse(description = "Plan item not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItemDocumentation = get {
    validUser { platformUser =>
      path(Segment / "documentation" / "planitems" / Segment) {
        (_, planItemId) => runQuery(caseQueries.getPlanItemDocumentation(planItemId, platformUser))
      }
    }
  }

  @Path("/{caseInstanceId}/documentation/casefile")
  @GET
  @Operation(
    summary = "Get the casefile documentation",
    description = "Returns a list with the documentation for case file items that are documented in the definition of the case",
    tags = Array("case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Case file documentation", responseCode = "200"),
      new ApiResponse(description = "No case file found for the case instance", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCaseFileDocumentation = get {
    validUser { platformUser =>
      path(Segment / "documentation" / "casefile") {
        caseInstanceId => runQuery(caseQueries.getCaseFileDocumentation(caseInstanceId, platformUser))
      }
    }
  }
}
