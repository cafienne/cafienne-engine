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
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.exception.MissingTenantException
import org.cafienne.cmmn.akka
import org.cafienne.cmmn.akka.command.debug.SwitchDebugMode
import org.cafienne.cmmn.akka.command.team.CaseTeam
import org.cafienne.cmmn.definition.InvalidDefinitionException
import org.cafienne.cmmn.instance.casefile.ValueList
import org.cafienne.cmmn.repository.MissingDefinitionException
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.service.api
import org.cafienne.service.api.cases._
import org.cafienne.service.api.cases.table.CaseRecord
import org.cafienne.service.api.model.StartCase
import org.cafienne.service.api.projection.CaseSearchFailure

import scala.util.{Failure, Success}

@Api(tags = Array("case"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute with CaseReader {

  override def routes = {
    getCases ~
      getUserCases ~
      stats ~
      getCase ~
      startCase ~
      debugCase
  }

  @GET
  @Operation(
    summary = "Get a list of cases",
    description = "Returns a list of case instances",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the cases from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "offset", description = "Starting position", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0")),
      new Parameter(name = "numberOfResults", description = "Maximum number of cases to fetch", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100")),
      new Parameter(name = "state", description = "Optional state of the cases to fetch (e.g. Active or Completed)", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "definition", description = "Optional definition name of the cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "sortBy", description = "Field to sort on", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "sortOrder", description = "Sort direction", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
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
        parameters('tenant ?, 'offset ? 0, 'numberOfResults ? 100, 'definition ?, 'state ?, 'sortBy ?, 'sortOrder ?) {
          (optionalTenant, offset, numResults, definition, state, sortBy, sortOrder) =>
            optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
              onComplete(handleSyncedQuery(() => caseQueries.getCases(optionalTenant, offset, numResults, platformUser, definition, status = state), caseLastModified)) {
                case Success(value) => complete(StatusCodes.OK, caseInstanceToValueList(value))
                case Failure(err) => complete(StatusCodes.NotFound, err)
              }
            }
        }
      }
    }
  }

  @Path("/user")
  @GET
  @Operation(
    summary = "Get a list of current user cases",
    description = "Returns a list of case instances which the current user started or is a participant",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the cases from", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "offset", description = "Starting position", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0")),
      new Parameter(name = "numberOfResults", description = "Number of cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100")),
      new Parameter(name = "state", description = "State of the cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "definition", description = "Definition of the cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "sortBy", description = "Field to sort on", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "sortOrder", description = "Sort direction", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
    ),
    responses = Array(
      new ApiResponse(description = "Cases found", responseCode = "200"),
      new ApiResponse(description = "No cases found based on query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getUserCases = get {
    path("user") {
      validUser { platformUser =>
        parameters('tenant ?, 'offset ? 0, 'numberOfResults ? 100, 'definition ?, 'state ?, 'sortBy ?, 'sortOrder ?) {
          (tenant, offset, numResults, definition, state, sortBy, sortOrder) =>
            optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
              onComplete(handleSyncedQuery(() => caseQueries.getMyCases(tenant, offset, numResults, platformUser, definition, state), caseLastModified)) {
                case Success(value) => complete(StatusCodes.OK, caseInstanceToValueList(value))
                case Failure(err) => complete(StatusCodes.NotFound, err)
              }
            }
        }
      }
    }
  }

  private def caseInstanceToValueList(rows: Seq[CaseRecord]): ValueList = {
    val responseValues = new ValueList
    rows.foreach(row => {
      val caseInstanceJSON = row.toValueMap
      caseInstanceJSON.put("team", new ValueList())
      responseValues.add(caseInstanceJSON)
    })
    responseValues
  }

  @Path("/stats")
  @GET
  @Operation(
    summary = "Get statistics for all case definitions",
    description = "Returns statistics of all case definitions",
    tags = Array("case"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optionally provide a specific tenant to read the statistics in", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "offset", description = "Starting position", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "0")),
      new Parameter(name = "numberOfResults", description = "Number of cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer], defaultValue = "100")),
      new Parameter(name = "definition", description = "Definition of the cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "state", description = "State of the cases", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
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
        parameters('tenant ?, 'offset ? 0, 'numberOfResults ? 100, 'definition ?, 'state ?
        ) { (tenant, offset, numOfResults, definition, status) =>
          optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
            onComplete(handleSyncedQuery(() => caseQueries.getCasesStats(tenant, offset, numOfResults, platformUser, definition, status), caseLastModified)) {
              case Success(value) => complete(StatusCodes.OK, caseListToValueMap(value))
              case Failure(err) => complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
    }
  }

  private def caseListToValueMap(caseList: Seq[CaseList]): ValueList = {
    val responsValues = new ValueList
    caseList.foreach(v => responsValues.add(v.toValueMap))
    responsValues
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
      path(Segment) { caseInstanceId => {
        optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
          onComplete(handleSyncedQuery(() => caseQueries.getFullCaseInstance(caseInstanceId, platformUser), caseLastModified)) {
            case Success(value) => complete(StatusCodes.OK, value.toString)
            case Failure(_: CaseSearchFailure) => complete(StatusCodes.NotFound)
            case Failure(_) => complete(StatusCodes.InternalServerError)
          }
        }
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
      new ApiResponse(description = "Case is created and started", responseCode = "201"),
      new ApiResponse(description = "Case definition not available", responseCode = "400"),
      new ApiResponse(description = "Something went wrong", responseCode = "500")
    )
  )
  @RequestBody(description = "case", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[StartCase]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def startCase = post {
    pathEndOrSingleSlash {
      validUser { platformUser =>
        post {
          entity(as[StartCase]) { payload =>
            try {
              val tenant = payload.tenant match {
                case None => platformUser.defaultTenant // This will throw an IllegalArgumentException if the default tenant is not configured
                case Some(string) => string.isEmpty match {
                  case true => platformUser.defaultTenant
                  case false => payload.tenant.get
                }
              }
              val definitionsDocument = CaseSystem.config.repository.DefinitionProvider.read(platformUser.getTenantUser(tenant), payload.definition)
              val caseDefinition = definitionsDocument.getFirstCase

              val newCaseId = payload.caseInstanceId.fold(UUID.randomUUID().toString.replace("-", "_"))(cid => cid)
              val inputParameters = payload.inputs
              val caseTeam: CaseTeam = payload.caseTeam.fold(CaseTeam())(c => teamConverter(c))
              val debugMode = payload.debug.getOrElse(CaseSystem.config.actor.debugEnabled)
              askModelActor(new akka.command.StartCase(tenant, platformUser.getTenantUser(tenant), newCaseId, caseDefinition, inputParameters, caseTeam, debugMode))
            } catch {
              case e: MissingTenantException => complete(StatusCodes.BadRequest, e.getMessage)
              case e: MissingDefinitionException => complete(StatusCodes.BadRequest, e.getMessage)
              case e: InvalidDefinitionException => complete(StatusCodes.InternalServerError, e.getMessage)
              case other: Throwable => complete(StatusCodes.InternalServerError, other)
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
    validUser { platformUser =>
      path(Segment / "debug" / Segment) { (caseInstanceId, debugMode) =>
        askCase(platformUser, caseInstanceId, tenantUser => new SwitchDebugMode(tenantUser, caseInstanceId, debugMode == "true"))
      }
    }
  }
}
