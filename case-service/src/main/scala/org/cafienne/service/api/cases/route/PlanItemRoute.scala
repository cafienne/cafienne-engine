/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{path, _}
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.cmmn.akka.command.MakePlanItemTransition
import org.cafienne.cmmn.instance.Transition
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api
import org.cafienne.service.api.cases.{CaseQueries, CaseReader}
import org.cafienne.service.api.projection.SearchFailure

import scala.util.{Failure, Success}

@Api(tags = Array("case plan"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class PlanItemRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute with CaseReader {

  val caseFileRoute = new CaseFileRoute(caseQueries)(userCache)
  val caseTeamRoute = new CaseTeamRoute(caseQueries)(userCache)

  override def routes = {
      getPlanItems ~
      getPlanItem ~
      planItemTransition ~
      getPlanItemHistory
    }

  @Path("/{caseInstanceId}/planitems")
  @GET
  @Operation(
    summary = "Get the planitems for a case",
    description = "Get the planitems for the specified case instance",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      //      new Parameter(name = "planItemType", description = "Type of planItems to get", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      //      new Parameter(name = "status", description = "Status of the planItems to get", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String])),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
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
            case Success(value) => complete(StatusCodes.OK, value.toString)
            case Failure(_: SearchFailure) => complete(StatusCodes.NotFound)
            case Failure(err) => throw err
          }
        }
      }
    }
  }

  @Path("/{caseInstanceId}/planitems/{planItemId}")
  @GET
  @Operation(
    summary = "Get a planitem for a case by planItemId",
    description = "Get a planitem for the specified case instance by it's planItemId",
    tags = Array("case plan"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "planItemId", description = "Unique id of the planItem", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
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
            case Success(value) => complete(StatusCodes.OK, value.toString)
            case Failure(_: SearchFailure) => complete(StatusCodes.NotFound)
            case Failure(err) => throw err
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
    tags = Array("case plan"),
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
    tags = Array("case plan"),
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
          case Success(value) => complete(StatusCodes.OK, value.toString)
          case Failure(_: SearchFailure) => complete(StatusCodes.NotFound)
          case Failure(err) => throw err
        }
      }
    }
  }
}
