/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.akka.actor.serialization.json.Value
import org.cafienne.cmmn.akka.command.casefile.{CreateCaseFileItem, DeleteCaseFileItem, ReplaceCaseFileItem, UpdateCaseFileItem}
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.service.api
import org.cafienne.service.api.projection.query.CaseQueries

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseFileRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute {

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
      path(Segment / "casefile") {
        caseInstanceId => runQuery(caseQueries.getCaseFile(caseInstanceId, platformUser))
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
    casefileContentRoute("create", (platformUser, json, caseInstanceId, path) => askCase(platformUser, caseInstanceId, tenantUser => new CreateCaseFileItem(tenantUser, caseInstanceId, json, path)))
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
    casefileContentRoute("replace", (platformUser, json, caseInstanceId, path) => askCase(platformUser, caseInstanceId, tenantUser => new ReplaceCaseFileItem(tenantUser, caseInstanceId, json, path)))
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
    casefileContentRoute("update", (platformUser, json, caseInstanceId, path) => askCase(platformUser, caseInstanceId, tenantUser => new UpdateCaseFileItem(tenantUser, caseInstanceId, json, path)))
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
    casefileRoute("delete", (platformUser, caseInstanceId, path) => askCase(platformUser, caseInstanceId, tenantUser => new DeleteCaseFileItem(tenantUser, caseInstanceId, path)))
  }

  /**
    * Run the action (replace, update, create) for the user on the case instance with the path and the json posted.
    * @param action
    * @param subRoute
    * @return
    */
  private def casefileContentRoute(action: String, subRoute: (PlatformUser, Value[_], String, org.cafienne.cmmn.instance.casefile.Path) => Route): Route = {
    casefileRoute(action, (platformUser, caseInstanceId, path) => {
      entity(as[Value[_]]) { json => {
        subRoute(platformUser, json, caseInstanceId, path)
      }}
    })
  }

  /**
    * Run the action (replace, update, create, delete) for the user on the case instance with the path.
    * @param action
    * @param subRoute
    * @return
    */
  private def casefileRoute(action: String, subRoute: (PlatformUser, String, org.cafienne.cmmn.instance.casefile.Path) => Route): Route = {
    validUser { platformUser =>
      pathPrefix(Segment / "casefile" / action ) { caseInstanceId =>
        withCaseFilePath(path => subRoute(platformUser, caseInstanceId, path))
      }
    }
  }

  /**
    * Checks if the path ends or has more elements left, and returns any remains into a Cafienne Path object.
    * Empty path (either with or without slash results in an empty path object, referring to top level CaseFile)
    * @param subRoute
    * @return
    */
  private def withCaseFilePath(subRoute: org.cafienne.cmmn.instance.casefile.Path => Route): Route = {
    // Creating a "cafienne-path" will validate the syntax
    import org.cafienne.cmmn.instance.casefile.Path
    pathEndOrSingleSlash {
      subRoute(new Path(""))
    } ~ path(Remaining) { rawPath =>
      // Take the "raw" remaining string, and decode it, and make it a Cafienne CaseFile Path
      // Note: taking "Segment" or "Segments" instead of "Remaining" fails and returns 405 on paths like "abc[0 ",
      //  when parsing it to a Cafienne path the error message is more clear.
      import java.nio.charset.StandardCharsets
      val decodedRawPath = java.net.URLDecoder.decode(rawPath, StandardCharsets.UTF_8.name)
      subRoute(new Path(decodedRawPath))
    }
  }
}
