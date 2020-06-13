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
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.cmmn.akka.command.casefile.{CreateCaseFileItem, DeleteCaseFileItem, ReplaceCaseFileItem, UpdateCaseFileItem}
import org.cafienne.cmmn.instance.casefile._
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.service.api
import org.cafienne.service.api.cases.{CaseQueries, CaseReader}
import org.cafienne.service.api.projection.CaseSearchFailure

import scala.util.{Failure, Success}

@Api(tags = Array("case file"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseFileRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute with CaseReader {

  override def routes = {
      getCaseFile ~
      createCaseFileItem ~
      replaceCaseFileItem ~
      updateCaseFileItem ~
      deleteCaseFileItem
  }

  @Path("/{caseInstanceId}/casefile")
  @GET
  @Operation(
    summary = "Get the casefile",
    description = "Get the case file from the specified case instance",
    tags = Array("case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = api.CASE_LAST_MODIFIED, description = "Get after events have been processed", in = ParameterIn.HEADER, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "Case file found", responseCode = "200"),
      new ApiResponse(description = "No case file found for the case instance", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def getCaseFile = get {
    validUser { platformUser =>
      path(Segment / "casefile") { caseInstanceId => {
        optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
          onComplete(handleSyncedQuery(() => caseQueries.getCaseFile(caseInstanceId, platformUser), caseLastModified)) {
            case Success(caseFile) => complete(StatusCodes.OK, caseFile.toString)
            case Failure(_: CaseSearchFailure) => complete(StatusCodes.NotFound)
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
    tags = Array("case file"),
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
    validUser { platformUser =>
      path(Segment / "casefile" / "create" / RemainingPath) { (caseInstanceId, path) =>
        entity(as[Value[_]]) { json =>
          askCase(platformUser, caseInstanceId, tenantUser => new CreateCaseFileItem(tenantUser, caseInstanceId, json, path.toString))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/casefile/replace/{path}")
  @PUT
  @Operation(
    summary = "Replace a case file item",
    description = "Replace a case file item at the specified path",
    tags = Array("case file"),
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
    validUser { platformUser =>
      path(Segment / "casefile" / "replace" / RemainingPath) { (caseInstanceId, path) =>
        entity(as[Value[_]]) { json =>
          askCase(platformUser, caseInstanceId, tenantUser => new ReplaceCaseFileItem(tenantUser, caseInstanceId, json, path.toString))
        }
      }
    }
  }

  @Path("/{caseInstanceId}/casefile/update/{path}")
  @PUT
  @Operation(
    summary = "Update a case file item",
    description = "Update a case file item at the specified path",
    tags = Array("case file"),
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
  def updateCaseFileItem = put {
    validUser { platformUser =>
      path(Segment / "casefile" / "update" / RemainingPath) { (caseInstanceId, path) => {
        entity(as[Value[_]]) { json =>
          askCase(platformUser, caseInstanceId, tenantUser => new UpdateCaseFileItem(tenantUser, caseInstanceId, json, path.toString))
        }
      }
      }
    }
  }

  @Path("/{caseInstanceId}/casefile/delete/{path}")
  @DELETE
  @Operation(
    summary = "Delete a case file item",
    description = "Delete a case file item at the specified path",
    tags = Array("case file"),
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
    validUser { platformUser =>
      path(Segment / "casefile" / "delete" / RemainingPath) { (caseInstanceId, path) =>
        askCase(platformUser, caseInstanceId, tenantUser => new DeleteCaseFileItem(tenantUser, caseInstanceId, path.toString))
      }
    }
  }
}
