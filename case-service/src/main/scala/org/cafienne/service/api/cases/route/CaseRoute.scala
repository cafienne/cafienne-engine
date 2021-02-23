/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import java.util.UUID
import _root_.akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{path, _}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.cmmn.akka.command.StartCase
import org.cafienne.cmmn.akka.command.debug.SwitchDebugMode
import org.cafienne.cmmn.akka.command.team.CaseTeam
import org.cafienne.cmmn.definition.InvalidDefinitionException
import org.cafienne.cmmn.repository.MissingDefinitionException
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.service.api
import org.cafienne.service.api.cases._
import org.cafienne.service.api.model.StartCaseFormat
import org.cafienne.service.api.projection.query.{CaseFilter, CaseQueries}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute {

  override def routes = concat(getCase, getCases, stats, getCaseDefinition, startCase, debugCase)

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
      new ApiResponse(description = "Cases found", responseCode = "200"),
      new ApiResponse(description = "No cases found based on query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCases = get {
    pathEndOrSingleSlash {
      validUser { platformUser =>
        parameters('tenant ?, 'identifiers ?, 'offset ? 0, 'numberOfResults ? 100, 'caseName ?, 'definition ?, 'state ?, 'sortBy ?, 'sortOrder ?) {
          (tenant, identifiers, offset, numResults, caseName, definition, state, sortBy, sortOrder) =>
            val backwardsCompatibleNameFilter: Option[String] = caseName.fold(definition)(n => Some(n))
            val filter = CaseFilter(tenant, identifiers = identifiers, caseName = backwardsCompatibleNameFilter, status = state)
            runListQuery(caseQueries.getCases(platformUser, filter, Area(offset, numResults), Sort.withDefault(sortBy, sortOrder, "lastModified")))
        }
      }
    }
  }

  @Path("/stats")
  @GET
  @Operation(
    summary = "Get statistics for all case definitions",
    description = "Returns statistics of all case definitions",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the statistics in", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "offset", description = "Starting position", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0"), required = false),
      new Parameter(name = "numberOfResults", description = "Number of cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100"), required = false),
      new Parameter(name = "caseName", description = "Get cases with this name", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "state", description = "State of the cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Statistics found and returned", responseCode = "200", content = Array(new Content(array = new ArraySchema(schema = new Schema(implementation = classOf[CaseList]))))),
      new ApiResponse(description = "No cases found based on query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def stats = get {
    path("stats") {
      validUser { platformUser =>
        parameters('tenant ?, 'offset ? 0, 'numberOfResults ? 100, 'caseName ?, 'definition ?, 'state ?
        ) { (tenant, offset, numOfResults, caseName, definition, status) =>
          val backwardsCompatibleNameFilter: Option[String] = caseName.fold(definition)(n => Some(n))
          runListQuery(caseQueries.getCasesStats(platformUser, tenant, offset, numOfResults, backwardsCompatibleNameFilter, status))
        }
      }
    }
  }

  @Path("/{caseInstanceId}")
  @GET
  @Operation(
    summary = "Get a case instance by caseInstanceId",
    description = "Returns a case instance",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false)
    ),
    responses = Array(
      new ApiResponse(description = "Case found and returned", responseCode = "200"),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCase = get {
    validUser { platformUser =>
      path(Segment) {
        caseInstanceId => runQuery(caseQueries.getFullCaseInstance(caseInstanceId, platformUser))
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
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false)
    ),
    responses = Array(
      new ApiResponse(description = "Case definition found and returned", responseCode = "200"),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCaseDefinition = get {
    caseInstanceSubRoute("definition", (platformUser, caseInstanceId) => runXMLQuery(caseQueries.getCaseDefinition(caseInstanceId, platformUser)))
  }

  @POST
  @Operation(
    summary = "Start a case instance",
    description = "Returns the caseInstanceId of the started case",
    tags = Array("case"),
    responses = Array(
      new ApiResponse(description = "Case is created and started", responseCode = "201"),
      new ApiResponse(description = "Case definition not available", responseCode = "400"),
      new ApiResponse(description = "Something went wrong", responseCode = "500")
    )
  )
  @RequestBody(description = "case", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[StartCaseFormat]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def startCase = post {
    pathEndOrSingleSlash {
      validUser { platformUser =>
        post {
          entity(as[StartCaseFormat]) { payload =>
            try {
              val tenant = platformUser.resolveTenant(payload.tenant)
              val definitionsDocument = CaseSystem.config.repository.DefinitionProvider.read(platformUser, tenant, payload.definition)
              val caseDefinition = definitionsDocument.getFirstCase

              val newCaseId = payload.caseInstanceId.fold(UUID.randomUUID().toString.replace("-", "_"))(cid => cid)
              val inputParameters = payload.inputs
              val caseTeam: CaseTeam = payload.caseTeam.fold(CaseTeam())(c => teamConverter(c))
              val debugMode = payload.debug.getOrElse(CaseSystem.config.actor.debugEnabled)
              askCaseWithValidTeam(tenant, caseTeam.members, new StartCase(tenant, platformUser.getTenantUser(tenant), newCaseId, caseDefinition, inputParameters, caseTeam, debugMode))
            } catch {
              case e: MissingDefinitionException => complete(StatusCodes.BadRequest, e.getMessage)
              case e: InvalidDefinitionException => complete(StatusCodes.BadRequest, e.getMessage)
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
      new ApiResponse(description = "Something went wrong", responseCode = "500")
    )
  )
  @Produces(Array("application/json"))
  def debugCase = put {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("debug" / Segment) { debugMode =>
        askCase(platformUser, caseInstanceId, tenantUser => new SwitchDebugMode(tenantUser, caseInstanceId, debugMode == "true"))
      }
    }
  }
}
