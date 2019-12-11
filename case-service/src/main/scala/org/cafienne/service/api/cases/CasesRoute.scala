/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases

import java.util.UUID

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{path, _}
import akka.pattern.ask
import io.swagger.annotations._
import javax.ws.rs.{Consumes, DELETE, GET, POST, PUT, Path, Produces}
import org.cafienne.akka.actor.command.response.CommandFailure
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.cmmn.akka
import org.cafienne.cmmn.akka.command.{AddDiscretionaryItem, CaseCommand, CaseCommandModels, GetDiscretionaryItems, MakePlanItemTransition}
import org.cafienne.cmmn.akka.command.casefile.{CreateCaseFileItem, DeleteCaseFileItem, ReplaceCaseFileItem, UpdateCaseFileItem}
import org.cafienne.cmmn.akka.command.debug.SwitchDebugMode
import org.cafienne.cmmn.akka.command.response.CaseResponse
import org.cafienne.cmmn.akka.command.team.{CaseTeam, CaseTeamMember, PutTeamMember, RemoveTeamMember, SetCaseTeam}
import org.cafienne.cmmn.definition.InvalidDefinitionException
import org.cafienne.cmmn.instance.Transition
import org.cafienne.cmmn.instance.casefile.{JSONReader, StringValue, ValueList, ValueMap}
import org.cafienne.cmmn.repository.MissingDefinitionException
import org.cafienne.infrastructure.akka.http.CommandMarshallers._
import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.service.{Main, api}
import org.cafienne.service.api.AuthenticatedRoute
import org.cafienne.service.api.model.StartCase
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.akka.actor.command.exception.MissingTenantException
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.cmmn.akka.response.CaseResponseModels
import org.cafienne.identity.IdentityProvider

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

@Api(value = "cases", tags = Array("cases"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CasesRoute
(caseQueries: CaseQueries, caseRegion: ActorRef)
(implicit val system: ActorSystem, implicit val actorRefFactory: ActorRefFactory, override implicit val userCache: IdentityProvider)
  extends AuthenticatedRoute with CaseReader {

  implicit def executionContext: ExecutionContextExecutor = actorRefFactory.dispatcher

  override def routes =
    pathPrefix("cases") {
      getCases ~
        getUserCases ~
        stats ~
        getCase ~
        startCase ~
        debugCase ~
        getCaseFile ~
        createCaseFileItem ~
        replaceCaseFileItem ~
        updateCaseFileItem ~
        deleteCaseFileItem ~
        getPlanItems ~
        getPlanItem ~
        planItemTransition ~
        getPlanItemHistory ~
        retrieveDiscretionaryItem ~
        planDiscretionaryItem ~
        setCaseTeam ~
        addCaseTeamMember ~
        deleteCaseTeamMember
    }

  @GET
  @Operation(
    summary = "Get a list of cases",
    description = "Returns a list of case instances",
    tags = Array("cases"),
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
      validUser { user =>
        parameters('tenant ?, 'offset ? 0, 'numberOfResults ? 100, 'definition ?, 'state ?, 'sortBy ?, 'sortOrder ?) {
          (optionalTenant, offset, numResults, definition, state, sortBy, sortOrder) =>
            onComplete(caseQueries.getCases(optionalTenant, offset, numResults, user, definition, status = state)) {
              case Success(value) => complete(StatusCodes.OK, caseInstanceToValueList(value))
              case Failure(err) => complete(StatusCodes.NotFound, err)
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
    tags = Array("cases"),
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
      validUser { user =>
        parameters('tenant ?, 'offset ? 0, 'numberOfResults ? 100, 'definition ?, 'state ?, 'sortBy ?, 'sortOrder ?) {
          (tenant, offset, numResults, definition, state, sortBy, sortOrder) =>
            onComplete(caseQueries.getMyCases(tenant, offset, numResults, user, definition, state)) {
              case Success(value) => complete(StatusCodes.OK, caseInstanceToValueList(value))
              case Failure(err) => complete(StatusCodes.NotFound, err)
            }
        }
      }
    }
  }

  private def caseInstanceToValueList(rows: Seq[CaseInstance]): ValueList = {
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
    tags = Array("cases"),
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
      validUser { user =>
        parameters('tenant ?, 'offset ? 0, 'numberOfResults ? 100, 'definition ?, 'state ?
        ) { (tenant, offset, numOfResults, definition, status) =>
          onComplete(caseQueries.getCasesStats(tenant, offset, numOfResults, user, definition, status)) {
            case Success(value) => complete(StatusCodes.OK, caseListToValueMap(value))
            case Failure(err) => complete(StatusCodes.InternalServerError)
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
    tags = Array("cases"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "notBefore", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false)
    ),
    responses = Array(
      new ApiResponse(description = "Case found and returned", responseCode = "200"),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCase = get {
    validUser { user =>
      path(Segment) { caseInstanceId => {
        optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
          onComplete(handleSyncedQuery(() => _getCaseInstance(caseInstanceId, user), caseLastModified)) {
            case Success(Some(value)) => complete(StatusCodes.OK, value)
            case Success(None) => complete(StatusCodes.NotFound)
            case Failure(_) => complete(StatusCodes.InternalServerError)
          }
        }
      }
      }
    }
  }

  private def _getCaseInstance(caseInstanceId: String, user: PlatformUser): Future[Option[ValueMap]] = {
    val result = for {
      caseInstance <- caseQueries.getCaseInstance(caseInstanceId, user)
      caseTeam <- caseQueries.getCaseTeam(caseInstanceId, user)
      caseFile <- caseQueries.getCaseFile(caseInstanceId, user)
      planItems <- caseQueries.getPlanItems(caseInstanceId, user)

    } yield (caseInstance, caseTeam, caseFile, planItems)
    result.map { x => mapCaseDataToResponse(x._1, x._2, x._3, x._4) }
  }

  private def mapCaseDataToResponse(maybeCaseInstance: Option[CaseInstance], caseTeam: Seq[CaseInstanceTeamMember], maybeCaseFile: Option[CaseFile], planItems: Seq[PlanItem]): Option[ValueMap] = {
    def parseCaseFileToJSON(maybeFile: Option[CaseFile]): ValueMap = {
      val jsonString = maybeFile.map(f => f.data).getOrElse("{}")
      JSONReader.parse(jsonString)
    }

    maybeCaseInstance.map { caseInstance =>
      val v = caseInstance.toValueMap
      v.put("file", parseCaseFileToJSON(maybeCaseFile))

      v.put("team", teamAsJson(caseTeam))

      val planItemValueList = new ValueList
      planItems.foreach(item => planItemValueList.add(item.toValueMap))
      v.put("planitems", planItemValueList)
      v
    }
  }

  private def teamAsJson(caseTeam: Seq[CaseInstanceTeamMember]): ValueList = {
    val team = new ValueMap;
    caseTeam.foreach(member => {
      val json = team.`with`(member.userId)
      json.putRaw("user", member.userId)
      json.withArray("roles").add(new StringValue(member.role))
    })
    val usersList = new ValueList
    team.getValue.forEach((userId, value) => usersList.add(value))
    usersList
  }

  @POST
  @Operation(
    summary = "Start a case instance",
    description = "Returns the caseInstanceId of the started case",
    tags = Array("cases"),
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
      validUser { user =>
        post {
          entity(as[StartCase]) { payload =>
            try {
              val tenant = payload.tenant match {
                case None => CaseSystem.defaultTenant // This will throw an IllegalArgumentException if the default tenant is not configured
                case Some(string) => string.isEmpty match {
                  case true => CaseSystem.defaultTenant
                  case false => payload.tenant.get
                }
              }
              val definitionsDocument = CaseSystem.DefinitionProvider.read(user.getTenantUser(tenant), payload.definition)
              val caseDefinition = definitionsDocument.getFirstCase

              val newCaseId = payload.caseInstanceId.fold(UUID.randomUUID().toString.replace("-", "_"))(cid => cid)
              val inputParameters = payload.inputs
              val caseTeam = payload.caseTeam
              val debugMode = payload.debug.getOrElse(CaseSystem.debugEnabled)
              invokeCase(new akka.command.StartCase(tenant, user.getTenantUser(tenant), newCaseId, caseDefinition, inputParameters, caseTeam, debugMode))
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
    tags = Array("cases", "debug"),
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
    validUser { user =>
      path(Segment / "debug" / Segment) { (caseInstanceId, debugMode) =>
        askCase(user, caseInstanceId, user => new SwitchDebugMode(user, caseInstanceId, debugMode == "true"))
      }
    }
  }

  @Path("/{caseInstanceId}/casefile")
  @GET
  @Operation(
    summary = "Get the casefile",
    description = "Get the case file of case file item for the specified case instance",
    tags = Array("cases", "case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "caseFileItem", description = "Case file item to get from the case file", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = "index", description = "The n-th instance of the requested case file item", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[Integer])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "notBefore", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Case file found", responseCode = "200"),
      new ApiResponse(description = "No case file found for the case instance", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCaseFile = get {
    validUser { user =>
      path(Segment / "casefile") { caseInstanceId => {
        optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
          onComplete(handleSyncedQuery(() => caseQueries.getCaseFile(caseInstanceId, user), caseLastModified)) {
            case Success(Some(caseFile)) => complete(StatusCodes.OK, new ValueMap("file", caseFile.toValueMap))
            case Success(None) => complete(StatusCodes.NotFound)
            case Failure(err) => complete(StatusCodes.InternalServerError, err)
          }
        }
      }
      }
    }
  }

  @Path("/{caseInstanceId}/casefile/create/{path}")
  @POST
  @Operation(
    summary = "Create a new case file item",
    description = "Create a new case file item at the specified path",
    tags = Array("cases", "case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "path", description = "The path to create the item at", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Transition applied successfully", responseCode = "202"),
      new ApiResponse(description = "Unable to apply transition", responseCode = "500")
    )
  )
  @RequestBody(description = "Case file item to create in JSON format", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Map[String, _]]))))
  @Consumes(Array("application/json"))
  def createCaseFileItem = post {
    validUser { user =>
      path(Segment / "casefile" / "create" / Segment) { (caseInstanceId, path) =>
        entity(as[ValueMap]) { value =>
          askCase(user, caseInstanceId, user => new CreateCaseFileItem(user, caseInstanceId, value, path))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/casefile/replace/{path}")
  @PUT
  @Operation(
    summary = "Replace a case file item",
    description = "Replace a case file item at the specified path",
    tags = Array("cases", "case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "path", description = "The path of the item to replace", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Transition applied successfully", responseCode = "201"),
      new ApiResponse(description = "Unable to apply transition", responseCode = "500")
    )
  )
  @RequestBody(description = "Case file item to create in JSON format", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Map[String, _]]))))
  @Consumes(Array("application/json"))
  def replaceCaseFileItem = put {
    validUser { user =>
      path(Segment / "casefile" / "replace" / Segment) { (caseInstanceId, path) =>
        entity(as[ValueMap]) { value =>
          askCase(user, caseInstanceId, user => new ReplaceCaseFileItem(user, caseInstanceId, value, path))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/casefile/update/{path}")
  @PUT
  @Operation(
    summary = "Update a case file item",
    description = "Update a case file item at the specified path",
    tags = Array("cases", "case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "path", description = "The path of the item to update", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Transition applied successfully", responseCode = "201"),
      new ApiResponse(description = "Unable to apply transition", responseCode = "500")
    )
  )
  @RequestBody(description = "Case file item to update in JSON format", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[Map[String, _]]))))
  @Consumes(Array("application/json"))
  def updateCaseFileItem = put {
    validUser { user =>
      path(Segment / "casefile" / "update" / Segment) { (caseInstanceId, path) =>
        entity(as[ValueMap]) { value =>
          askCase(user, caseInstanceId, user => new UpdateCaseFileItem(user, caseInstanceId, value, path))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/casefile/delete/{path}")
  @DELETE
  @Operation(
    summary = "Delete a case file item",
    description = "Delete a case file item at the specified path",
    tags = Array("cases", "case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "path", description = "The path of the item to delete", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Case file item deleted sucessfully", responseCode = "204"),
      new ApiResponse(description = "Unable to apply transition", responseCode = "500")
    )
  )
  @Consumes(Array("application/json"))
  def deleteCaseFileItem = delete {
    validUser { user =>
      path(Segment / "casefile" / "delete" / Segment) { (caseInstanceId, path) =>
        askCase(user, caseInstanceId, user => new DeleteCaseFileItem(user, caseInstanceId, path))
      }
    }
  }

  @Path("/{caseInstanceId}/planitems")
  @GET
  @Operation(
    summary = "Get the planitems for a case",
    description = "Get the planitems for the specified case instance",
    tags = Array("cases", "case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      //      new Parameter(name = "planItemType", description = "Type of planItems to get", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      //      new Parameter(name = "status", description = "Status of the planItems to get", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "notBefore", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "PlanItems found", responseCode = "200"),
      new ApiResponse(description = "No PlanItems found based on the query params", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItems = get {
    validUser { user =>
      path(Segment / "planitems") { caseInstanceId =>
        optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
          //          parameters(planItemType ?, 'status ?) { (planItemType, status) =>
          // planItemType and status are removed!!
          onComplete(handleSyncedQuery(() => caseQueries.getPlanItems(caseInstanceId, user), caseLastModified)) {
            case Success(value) => complete(StatusCodes.OK, planItemToValueList(value))
            case Failure(err) => complete(StatusCodes.InternalServerError, err)
          }
        }
      }
    }
  }

  private def planItemToValueList(items: Seq[PlanItem]): ValueList = {
    val responseValues = new ValueList
    items.foreach(item => responseValues.add(item.toValueMap))
    responseValues
  }

  @Path("/{caseInstanceId}/planitems/{planItemId}")
  @GET
  @Operation(
    summary = "Get a planitem for a case by planItemId",
    description = "Get a planitem for the specified case instance by it's planItemId",
    tags = Array("cases", "case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "planItemId", description = "Unique id of the planItem", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "notBefore", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Plan item found", responseCode = "200"),
      new ApiResponse(description = "Plan item not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItem = get {
    validUser { user =>
      path(Segment / "planitems" / Segment) { (_, planItemId) =>
        optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
          onComplete(handleSyncedQuery(() => caseQueries.getPlanItem(planItemId, user), caseLastModified)) {
            case Success(Some(value)) => complete(StatusCodes.OK, value.toValueMap)
            case Success(None) => complete(StatusCodes.NotFound)
            case Failure(err) => complete(StatusCodes.InternalServerError, err)
          }
        }
      }
    }
  }

  @Path("/{caseInstanceId}/planitems/{planItemId}/{transition}")
  @POST
  @Operation(
    summary = "Apply a transition on a planItem",
    description = "Applies a transition to the planItem for the given caseInstanceId",
    tags = Array("cases", "case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "planItemId", description = "Unique id of the planItem", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "transition", description = "Transition to apply", in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String], allowableValues = Array("complete", "close", "create", "enable", "disable", "exit", "fault", "manualStart", "occur", "parentResume", "parentSuspend", "reactivate", "reenable", "resume", "start", "suspend", "terminate")),
        required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Transition applied successfully", responseCode = "202"),
      new ApiResponse(description = "Unable to apply transition", responseCode = "500")
    )
  )
  @Produces(Array("application/json"))
  def planItemTransition = post {
    validUser { user =>
      path(Segment / "planitems" / Segment / Segment) { (caseInstanceId, planItemId, transitionString) =>
        val transition = Transition.getEnum(transitionString)
        askCase(user, caseInstanceId, user => new MakePlanItemTransition(user, caseInstanceId, planItemId, transition, ""))
      }
    }
  }

  @Path("/{caseInstanceId}/planitems/{planItemId}/history")
  @GET
  @Operation(
    summary = "Get history of a planitem for a case by planItemId",
    description = "Get history of a planitem for the specified case instance by it's planItemId",
    tags = Array("cases", "case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "planItemId", description = "Unique id of the planItem", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "PlanItem found", responseCode = "200"),
      new ApiResponse(description = "No PlanItem found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getPlanItemHistory = get {
    validUser { user =>
      path(Segment / "planitems" / Segment / "history") { (caseInstanceId, planItemId) =>
        onComplete(caseQueries.getPlanItemHistory(planItemId, user)) {
          case Success(value) => complete(StatusCodes.OK, planItemHistoryToValueList(value))
          case Failure(err) => complete(StatusCodes.NotFound, err)
        }
      }
    }
  }

  private def planItemHistoryToValueList(items: Seq[PlanItemHistory]): ValueList = {
    val responseValues = new ValueList
    items.foreach(item => responseValues.add(item.toValueMap))
    responseValues
  }

  @Path("/{caseInstanceId}/discretionaryitems")
  @GET
  @Operation(
    summary = "Get a list of currently applicable discretionary items",
    description = "Returns a list of discretionary items with respect to the current state of the case instance and the user roles",
    tags = Array("cases", "discretionary items"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String])),
    ),
    responses = Array(
      new ApiResponse(description = "Items found and returned", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseModels.DiscretionaryItemsList])))),
      new ApiResponse(description = "No items found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def retrieveDiscretionaryItem = get {
    validUser { user =>
      path(Segment / "discretionaryitems") { caseInstanceId =>
        askCase(user, caseInstanceId, user => new GetDiscretionaryItems(user, caseInstanceId))
      }
    }
  }

  @Path("/{caseInstanceId}/discretionaryitems/plan")
  @POST
  @Operation(
    summary = "Plan a discretionary item",
    description = "Plan a discretionary item for the provided case instance",
    tags = Array("cases", "discretionary items"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to set a case team has been accepted", responseCode = "201", content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseModels.PlannedDiscretionaryItem])))),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @RequestBody(description = "Item to be planned", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseResponseModels.PlannedDiscretionaryItem]))))
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def planDiscretionaryItem = post {
    validUser { user =>
      path(Segment / "discretionaryitems" / "plan") { caseInstanceId =>
        entity(as[CaseCommandModels.PlanDiscretionaryItem]) { payload =>
          askCase(user, caseInstanceId, user => new AddDiscretionaryItem(user, caseInstanceId, payload.name, payload.definitionId, payload.parentId, payload.planItemId.orNull))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/caseteam")
  @POST
  @Operation(
    summary = "Sets a new case team",
    description = "Sets a new case team for a case instance",
    tags = Array("cases", "case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to set a case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @RequestBody(description = "Case team in JSON format", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseTeam]))))
  @Consumes(Array("application/json"))
  def setCaseTeam = post {
    validUser { user =>
      path(Segment / "caseteam") { caseInstanceId =>
        entity(as[CaseTeam]) { caseTeam =>
          askCase(user, caseInstanceId, user => new SetCaseTeam(user, caseInstanceId, caseTeam))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/caseteam")
  @PUT
  @Operation(
    summary = "Add or update a case team member",
    description = "Add a new case team member or change the roles of an existing member",
    tags = Array("cases", "case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to update a case team has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @RequestBody(description = "Case Team Member", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[CaseTeamMember]))))
  @Consumes(Array("application/json"))
  def addCaseTeamMember = put {
    validUser { user =>
      path(Segment / "caseteam") { caseInstanceId =>
        entity(as[CaseTeamMember]) { caseTeamMember =>
          askCase(user, caseInstanceId, user => new PutTeamMember(user, caseInstanceId, caseTeamMember))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/caseteam/{userId}")
  @DELETE
  @Operation(
    summary = "Delete a case team member",
    description = "Delete a case team member",
    tags = Array("cases", "case team"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "userId", description = "Unique id of the case team member", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Your request to delete a case team member has been accepted", responseCode = "202"),
      new ApiResponse(description = "Case not found", responseCode = "404"),
      new ApiResponse(description = "Internal server error", responseCode = "500")
    )
  )
  @Consumes(Array("application/json"))
  def deleteCaseTeamMember = delete {
    validUser { user =>
      path(Segment / "caseteam" / Segment) { (caseInstanceId, userId) =>
        askCase(user, caseInstanceId, user => new RemoveTeamMember(user, caseInstanceId, userId))
      }
    }
  }

  def askCase(platformUser: PlatformUser, caseInstanceId: String, createTaskCommand: CreateCaseCommand) = {
    onComplete(caseQueries.getTenantInformation(caseInstanceId, platformUser)) {
      case Success(retrieval) => {
        retrieval match {
          case Some(tenant) => invokeCase(createTaskCommand.apply(platformUser.getTenantUser(tenant)))
          case None => complete(StatusCodes.NotFound, "A case with id " + caseInstanceId + " cannot be found in the system")
        }
      }
      case Failure(error) => complete(StatusCodes.InternalServerError, error)
    }
  }

  implicit val timeout = Main.caseSystemTimeout

  def invokeCase(command: CaseCommand) = {
    onComplete(caseRegion ? command) {
      case Success(value) =>
        value match {
          case e: CommandFailure =>
            // This should probably return something like "Command not accepted", so not 500 but something 400
            complete(StatusCodes.BadRequest, e.exception.getMessage)
          case value: CaseResponse =>
            respondWithHeader(RawHeader(api.CASE_LAST_MODIFIED, value.lastModifiedContent().toString)) {
              complete(StatusCodes.OK, value)
            }
          case TestResponse => complete(StatusCodes.OK)
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }

  trait CreateCaseCommand {
    def apply(user: TenantUser): CaseCommand
  }

}
