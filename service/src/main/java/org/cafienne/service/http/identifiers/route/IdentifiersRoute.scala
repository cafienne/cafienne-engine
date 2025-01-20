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

package org.cafienne.service.http.identifiers.route

import org.apache.pekko.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.IdentifierQueriesImpl
import org.cafienne.persistence.querydb.query.filter.IdentifierFilter
import org.cafienne.service.http.cases.CasesRoute
import org.cafienne.service.http.identifiers.model.BusinessIdentifierFormat
import org.cafienne.system.CaseSystem

import jakarta.ws.rs._

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/identifiers")
class IdentifiersRoute(override val caseSystem: CaseSystem) extends CasesRoute {
  val identifierQueries = new IdentifierQueriesImpl

  override def routes: Route = concat(getIdentifiers, getIdentifierNames)

  @GET
  @Operation(
    summary = "Get a list of identifiers",
    description = "Returns a list of identifiers",
    tags = Array("identifiers"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the identifiers from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "name", description = "Optionally provide a name to read only identifiers with that name", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "offset", description = "Starting position", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0"), required = false),
      new Parameter(name = "numberOfResults", description = "Maximum number of identifiers to fetch", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100"), required = false),
      new Parameter(name = "sortBy", description = "Field to sort on", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "sortOrder", description = "Sort direction ('asc' or 'desc')", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of identifiers", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[BusinessIdentifierFormat]))))),
    )
  )
  @Produces(Array("application/json"))
  def getIdentifiers: Route = get {
    pathEndOrSingleSlash {
      caseUser { user =>
        parameters("tenant".?, "name".?, "offset".?(0), "numberOfResults".?(100), "sortBy".?, "sortOrder".?) {
          (tenant, name, offset, numResults, sortBy, sortOrder) =>
            val filter = IdentifierFilter(tenant, name)
            runQuery(identifierQueries.getIdentifiers(user, filter, Area(offset, numResults), Sort.withDefault(sortBy, sortOrder, "name")))
        }
      }
    }
  }

  @Path("/names")
  @GET
  @Operation(
    summary = "Get a list of identifier names",
    description = "Returns a list of identifier names, i.e. names for which identifier values exist",
    tags = Array("identifiers"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the identifiers from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "List of identifier names", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[String]))))),
    )
  )
  @Produces(Array("application/json"))
  def getIdentifierNames: Route = get {
    path("names") {
      pathEndOrSingleSlash {
        caseUser { user =>
          parameters("tenant".?) { tenant =>
            runListQuery(identifierQueries.getIdentifierNames(user, tenant))
          }
        }
      }
    }
  }
}
