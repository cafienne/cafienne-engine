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
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.cmmn.akka.command.migration.{CompleteMigration, MigrateDefinition}
import org.cafienne.cmmn.instance.migration.MigrationScript
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.service.api
import org.cafienne.service.api.model.{CompleteMigrationFormat, MigrationDefinitionFormat}
import org.cafienne.service.api.projection.query.CaseQueries

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseMigrationRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute {

  override def routes = {
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
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Migration started", responseCode = "200"),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def startMigration = post {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("migrate-definition") {
        entity(as[MigrationDefinitionFormat]) { migrateDefinition =>
          val definitionsDocument = CaseSystem.config.repository.DefinitionProvider.read(platformUser, "", migrateDefinition.newDefinition)
          val caseDefinition = definitionsDocument.getFirstCase
          askCase(platformUser, caseInstanceId, tenantUser => new MigrateDefinition(tenantUser, caseInstanceId, caseDefinition, new MigrationScript(migrateDefinition.migrationScript)))
        }
      }
    }
  }
}
