/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route

import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.cmmn.actorapi.command.migration.MigrateDefinition
import org.cafienne.infrastructure.Cafienne
import org.cafienne.service.akkahttp.Headers
import org.cafienne.service.akkahttp.cases.model.CaseMigrationAPI._
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseMigrationRoute(override val caseSystem: CaseSystem) extends CasesRoute {
  override def routes: Route = {
      startMigration
    }

  @Path("/{caseInstanceId}/migrate-definition")
  @POST
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
  @RequestBody(description = "case", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[MigrationDefinitionFormat]))))
  @Produces(Array("application/json"))
  def startMigration: Route = post {
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("migrate-definition") {
        entity(as[MigrationDefinitionFormat]) { migrateDefinition =>
          val definitionsDocument = Cafienne.config.repository.DefinitionProvider.read(user, "", migrateDefinition.newDefinition)
          val caseDefinition = definitionsDocument.getFirstCase
          askCase(user, caseInstanceId, tenantUser => new MigrateDefinition(tenantUser, caseInstanceId, caseDefinition))
        }
      }
    }
  }
}
