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
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.cmmn.actorapi.command.migration.MigrateDefinition
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.akka.http.authentication.IdentityProvider
import org.cafienne.service.api.Headers
import org.cafienne.service.api.cases.model.CaseMigrationAPI._
import org.cafienne.service.db.query.CaseQueries
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseMigrationRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CasesRoute {

  override def routes: Route = {
      startMigration
    }

  @Path("/{caseInstanceId}/migration/start")
  @GET
  @Operation(
    summary = "Start migration of a case to a new definition",
    description = "Start migration of a case to a new definition",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Migration started", responseCode = "200"),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def startMigration: Route = post {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("migrate-definition") {
        entity(as[MigrationDefinitionFormat]) { migrateDefinition =>
          val definitionsDocument = Cafienne.config.repository.DefinitionProvider.read(platformUser, "", migrateDefinition.newDefinition)
          val caseDefinition = definitionsDocument.getFirstCase
          askCase(platformUser, caseInstanceId, tenantUser => new MigrateDefinition(tenantUser, caseInstanceId, caseDefinition))
        }
      }
    }
  }
}
