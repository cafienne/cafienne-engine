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

package org.cafienne.service.http.cases

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs._
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.cmmn.actorapi.command.StartCase
import org.cafienne.cmmn.actorapi.command.debug.SwitchDebugMode
import org.cafienne.cmmn.actorapi.command.team.CaseTeam
import org.cafienne.cmmn.definition.InvalidDefinitionException
import org.cafienne.cmmn.repository.MissingDefinitionException
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.infrastructure.lastmodified.Headers
import org.cafienne.persistence.querydb.query.cmmn.filter.CaseFilter
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.http.cases.CaseAPIFormat._
import org.cafienne.service.infrastructure.route.CaseTeamValidator

import java.util.UUID
import scala.util.{Failure, Success}

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/cases")
class CaseRoute(override val httpService: CaseEngineHttpServer) extends CasesRoute with CaseTeamValidator {
  override def routes: Route = concat(getCases, /*stats,*/ getCase, getCaseDefinition, startCase, debugCase)

  @GET
  @Operation(
    summary = "Get a list of cases",
    description = "Returns a list of case instances",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the cases from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "identifiers", description = "Comma separated string of business identifiers with values", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "state", description = "Get cases with this state, e.g. Active or Completed (optional)", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "caseName", description = "Get cases with this name (optional)", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "offset", description = "Starting position", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0"), required = false),
      new Parameter(name = "numberOfResults", description = "Maximum number of cases to fetch", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100"), required = false),
      new Parameter(name = "sortBy", description = "Field to sort on", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "sortOrder", description = "Sort direction ('asc' or 'desc')", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Cases found", responseCode = "200", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[CaseSummaryResponseFormat]))))),
      new ApiResponse(description = "No cases found based on query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCases: Route = get {
    pathEndOrSingleSlash {
      caseUser { user =>
        parameters("tenant".?,"identifiers".?, "offset".?(0), "numberOfResults".?(100), "caseName".?, "definition".?, "state".?, "sortBy".?, "sortOrder".?) {
          (tenant, identifiers, offset, numResults, caseName, definition, state, sortBy, sortOrder) =>
            val backwardsCompatibleNameFilter: Option[String] = caseName.fold(definition)(n => Some(n))
            val filter = CaseFilter(tenant, identifiers = identifiers, caseName = backwardsCompatibleNameFilter, status = state)
            runListQuery(caselistQueries.getCases(user, filter, Area(offset, numResults), Sort.withDefault(sortBy, sortOrder, "lastModified")))
        }
      }
    }
  }

//  @Path("/stats")
//  @GET
//  @Operation(
//    summary = "Get statistics for all case definitions",
//    description = "Returns statistics of all case definitions",
//    tags = Array("case"),
//    parameters = Array(
//      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the statistics in", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
//      new Parameter(name = "offset", description = "Starting position", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0"), required = false),
//      new Parameter(name = "numberOfResults", description = "Number of cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100"), required = false),
//      new Parameter(name = "caseName", description = "Get cases with this name", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
//      new Parameter(name = "state", description = "State of the cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
//    ),
//    responses = Array(
//      new ApiResponse(description = "Statistics found and returned", responseCode = "200", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[CaseList]))))),
//      new ApiResponse(description = "No cases found based on query params", responseCode = "404")
//    )
//  )
//  @Produces(Array("application/json"))
//  def stats: Route = get {
//    path("stats") {
//      validUser { platformUser =>
//        parameters("tenant".?, "offset".?(0), "numberOfResults".?(100), "caseName".?, "definition".?, "state".?
//        ) { (tenant, offset, numOfResults, caseName, definition, status) =>
//          val backwardsCompatibleNameFilter: Option[String] = caseName.fold(definition)(n => Some(n))
//          runListQuery(caseQueries.getCasesStats(platformUser, tenant, offset, numOfResults, backwardsCompatibleNameFilter, status))
//        }
//      }
//    }
//  }

  @Path("/{caseInstanceId}")
  @GET
  @Operation(
    summary = "Get a case instance by caseInstanceId",
    description = "Returns a case instance",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false)
    ),
    responses = Array(
      new ApiResponse(description = "Case found and returned", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseFormat])))),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCase: Route = get {
    caseUser { user =>
      path(Segment) {
        caseInstanceId => runQuery(caseInstanceQueries.getFullCaseInstance(caseInstanceId, user))
      }
    }
  }

  @Path("/{caseInstanceId}/definition")
  @GET
  @Operation(
    summary = "Get the definition of a case instance",
    description = "Returns the definition of a case instance",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = Headers.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false)
    ),
    responses = Array(
      new ApiResponse(description = "Case definition found and returned",  responseCode = "200", content = Array(new Content(mediaType = "text/xml", schema = new Schema(implementation = classOf[CaseDefinitionFormat])))),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("text/xml"))
  def getCaseDefinition: Route = get {
    caseInstanceSubRoute("definition") { (user, caseInstanceId) =>
      readLastModifiedHeader() { lastModified =>
        onComplete(runSyncedQuery(caseInstanceQueries.getCaseDefinition(caseInstanceId, user), lastModified)) {
          case Success(record) => complete(StatusCodes.OK, HttpEntity(ContentTypes.`text/xml(UTF-8)`, record.content))
          case Failure(t) => handleFailure(t)
        }
      }
    }
  }

  @POST
  @Operation(
    summary = "Start a case instance",
    description = "Returns the caseInstanceId of the started case",
    tags = Array("case"),
    responses = Array(
      new ApiResponse(description = "Case is created and started", responseCode = "201", content = Array(new Content(schema = new Schema(implementation = classOf[StartCaseResponse])))),
      new ApiResponse(description = "Case definition not available", responseCode = "400"),
    )
  )
  @RequestBody(description = "case", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[StartCaseFormat]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def startCase: Route = post {
    pathEndOrSingleSlash {
      caseUser { user =>
        post {
          entity(as[StartCaseFormat]) { payload =>
            caseStarter(user, payload.tenant) { (user, tenant) =>
              try {
                val definitionsDocument = caseSystem.config.repository.DefinitionProvider.read(user, tenant, payload.definition)
                val caseDefinition = definitionsDocument.getFirstCase

                val newCaseId = payload.caseInstanceId.fold(UUID.randomUUID().toString.replace("-", "_"))(cid => cid)
                val inputParameters = payload.inputs
                val caseTeam: CaseTeam = payload.caseTeam.asTeam
                val debugMode = payload.debug.getOrElse(caseSystem.config.actor.debugEnabled)
                validateTenantAndTeam(caseTeam, tenant, team => askModelActor(new StartCase(tenant, user, newCaseId, caseDefinition, inputParameters, team, debugMode)))
              } catch {
                case e: MissingDefinitionException => complete(StatusCodes.BadRequest, e.getMessage)
                case e: InvalidDefinitionException => complete(StatusCodes.BadRequest, e.getMessage)
              }
            }
          }
        }
      }
    }
  }

  @Path("/{caseInstanceId}/debug/{debugMode}")
  @PUT
  @Operation(
    summary = "Enable or disable debug mode for the case",
    description = "Enable or disable debug mode for the case",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "debugMode", description = "false - disables debug mode, true enables", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[Boolean]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Case is switched from/to debug mode", responseCode = "200"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
    )
  )
  @Produces(Array("application/json"))
  def debugCase: Route = put {
    caseInstanceSubRoute { (user, caseInstanceId) =>
      path("debug" / Segment) { debugMode =>
        askCase(user, caseInstanceId, caseMember => new SwitchDebugMode(caseMember, caseInstanceId, caseMember.rootCaseId, debugMode == "true"))
      }
    }
  }
}
