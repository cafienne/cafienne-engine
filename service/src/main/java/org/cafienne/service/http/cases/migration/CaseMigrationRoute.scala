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

package org.cafienne.service.http.cases.migration

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.cmmn.actorapi.command.migration.MigrateDefinition
import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.infrastructure.lastmodified.Headers
import org.cafienne.service.http.cases.CasesRoute
import org.cafienne.service.http.cases.migration.CaseMigrationAPI._
import org.cafienne.system.CaseSystem

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
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
          val newDefinition = definitionsDocument.getFirstCase
          val newCaseTeam = migrateDefinition.newTeam.map(_.asTeam).orNull
          askCase(user, caseInstanceId, caseMember => new MigrateDefinition(caseMember, caseInstanceId, newDefinition, newCaseTeam))
        }
      }
    }
  }
}
