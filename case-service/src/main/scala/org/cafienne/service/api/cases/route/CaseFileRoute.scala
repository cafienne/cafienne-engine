/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.model.{Multipart, StatusCodes}
import akka.http.scaladsl.model.Multipart.BodyPart
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import javax.ws.rs._
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.exception.InvalidCommandException
import org.cafienne.akka.actor.command.response.{CommandFailure, ModelResponse, SecurityFailure}
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.akka.actor.serialization.json.{JSONReader, Value, ValueList}
import org.cafienne.cmmn.akka.command.casefile.document.{AddDocumentInformation, GetDownloadInformation, GetUploadInformation}
import org.cafienne.cmmn.akka.command.casefile.{CreateCaseFileItem, DeleteCaseFileItem, ReplaceCaseFileItem, UpdateCaseFileItem}
import org.cafienne.cmmn.akka.command.response.CaseResponse
import org.cafienne.cmmn.instance.casefile.document.StorageResult
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.service.{Main, api}
import org.cafienne.service.api.projection.query.CaseQueries

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CaseFileRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute {

  override def routes = concat(getCaseFile, createCaseFileItem, replaceCaseFileItem, updateCaseFileItem, deleteCaseFileItem, uploadCaseFileDocument, getUploadInformation, getDownloadInformation, downloadCaseFileDocument)

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
    casefileContentRoute("create", (platformUser, json, caseInstanceId, path) => askCase(platformUser, caseInstanceId, tenantUser => new CreateCaseFileItem(tenantUser, caseInstanceId, path, json)))
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
    casefileContentRoute("replace", (platformUser, json, caseInstanceId, path) => askCase(platformUser, caseInstanceId, tenantUser => new ReplaceCaseFileItem(tenantUser, caseInstanceId, path, json)))
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
    casefileContentRoute("update", (platformUser, json, caseInstanceId, path) => askCase(platformUser, caseInstanceId, tenantUser => new UpdateCaseFileItem(tenantUser, caseInstanceId, path, json)))
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

  @Path("/{caseInstanceId}/casefile/upload-information")
  @GET
  @Operation(
    summary = "Retrieve upload information from the case file",
    description = "Returns a metadata description of the type of information that can be uploaded into the case file",
    tags = Array("case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Case file upload information", responseCode = "200"),
    )
  )
  def getUploadInformation = get {
    casefileRoute("upload-information", (platformUser, caseInstanceId, path) => {
      path.isEmpty match {
        case true => askCase(platformUser, caseInstanceId, tenantUser => new GetUploadInformation(tenantUser, caseInstanceId))
        case false => complete(StatusCodes.BadRequest, "A request for upload information cannot carry path information")
      }
    })
  }

  @Path("/{caseInstanceId}/casefile/download-information")
  @GET
  @Operation(
    summary = "Retrieve download information for the case",
    description = "Returns a metadata description of the documents that can be downloaded from the case file",
    tags = Array("case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Case file download information", responseCode = "200"),
    )
  )
  def getDownloadInformation = get {
    casefileRoute("download-information", (platformUser, caseInstanceId, path) => {
      path.isEmpty match {
        case true => askCase(platformUser, caseInstanceId, tenantUser => new GetDownloadInformation(tenantUser, caseInstanceId))
        case false => complete(StatusCodes.BadRequest, "A request for upload information cannot carry path information")
      }
    })
  }

  @Path("/{caseInstanceId}/casefile/upload/{path}")
  @POST
  @Operation(
    summary = "Upload a document into a case file item",
    description = "Upload a document into the case file item at the specified path",
    tags = Array("case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "path", description = "The path of the item to delete", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Case file item uploaded sucessfully", responseCode = "204"),
    )
  )
  @Consumes(Array("application/json", "multipart-mime"))
  def uploadCaseFileDocument = post {
    casefileRoute("upload", (platformUser, caseInstanceId, path) => {
      validateCaseAccess(platformUser, caseInstanceId, tenantUser => {
        path.isEmpty match {
          case true => complete(StatusCodes.BadRequest, "Cannot upload document(s) without a CaseFileItem path")
          case false => extractRequestContext { ctx =>
            entity(as[Multipart.FormData]) { formData =>
              val storage = CaseSystem.config.engine.documentService.DocumentService
              implicit val materializer = ctx.materializer
              // collect all parts of the multipart as it arrives into a map
              val allPartsF: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](1) {
                case b: BodyPart if b.name == "file" => {
                  val contentType = b.entity.contentType.toString()
                  storage.upload(tenantUser, caseInstanceId, path, b).map(identifier => identifier.identifier -> new StorageResult(identifier, contentType))
                }
                case b: BodyPart if b.name == "content" => b.toStrict(2.seconds).map(strict => b.name -> JSONReader.parse(strict.entity.data.utf8String))
                case b: BodyPart => throw new InvalidCommandException("Cannot upload document - encountered unknown form element " + b.name)
              }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)

              // After all documents have been uploaded to the DocumentStorage, we need to inform the case instance on the result
              //  The case may or may not accept the uploads; if not, then we'll instruct the DocumentStorage to remove the documents
              onSuccess(allPartsF) { allParts =>
                val storageResults: Seq[StorageResult] = allParts.filter(part => part._2.isInstanceOf[StorageResult]).map(_._2.asInstanceOf[StorageResult]).toSeq
                val list: ValueList = new ValueList()
                allParts.filter(part => part._2.isInstanceOf[Value[_]]).map(_._2.asInstanceOf[Value[_]]).foreach(v => list.add(v))

                import akka.pattern.ask
                implicit val timeout = Main.caseSystemTimeout

                val command = new AddDocumentInformation(tenantUser, caseInstanceId, path, list, storageResults.toArray)
                onComplete(CaseSystem.router ? command) {
                  case Failure(exception) => {
                    logger.info(s"Encountered an unexpected exception of type " + exception.getClass.getName +", removing uploaded documents")
                    logger.whenDebugEnabled(() => {
                      logger.debug("Exception: ", exception)
                    })
                    storage.removeUploads(tenantUser, caseInstanceId, path, storageResults.map(r => r.identifier))
                    complete(StatusCodes.InternalServerError, exception.getMessage)
                  }
                  case Success(response) => {
                    response match {
                      case s: SecurityFailure => {
                        storage.removeUploads(tenantUser, caseInstanceId, path, storageResults.map(r => r.identifier))
                        complete(StatusCodes.Unauthorized, s.exception.getMessage)
                      }
                      case e: CommandFailure => {
                        storage.removeUploads(tenantUser, caseInstanceId, path, storageResults.map(r => r.identifier))
                        complete(StatusCodes.BadRequest, e.exception.getMessage)
                      }
                      case value: CaseResponse => {
                        import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
                        writeLastModifiedHeader(value) {
                          complete(StatusCodes.OK, value)
                        }
                      }
                      case other => {
                        logger.info(s"Uploading documents resulted in an unknown response of type ${other.getClass.getName}; instructing storage to clear documents")
                        storage.removeUploads(tenantUser, caseInstanceId, path, storageResults.map(r => r.identifier))
                        complete(StatusCodes.InternalServerError)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      })
    })
  }

  @Path("/{caseInstanceId}/casefile/download/{path}")
  @GET
  @Operation(
    summary = "Download the case file document at the path",
    description = "Download a document from the case file item at the specified path",
    tags = Array("case file"),
    parameters = Array(
      new Parameter(name = "caseInstanceId", description = "Unique id of the case instance", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
      new Parameter(name = "path", description = "The path of the document to be downloaded", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Document contents", responseCode = "200"),
    )
  )
  def downloadCaseFileDocument = get {
    casefileRoute("download", (platformUser, caseInstanceId, path) => {
      validateCaseAccess(platformUser, caseInstanceId, tenantUser => {
        val storage = CaseSystem.config.engine.documentService.DocumentService
        storage.download(tenantUser, caseInstanceId, path)
      })
    })
  }

  /**
    * Run the action (replace, update, create) for the user on the case instance with the path and the json posted.
    *
    * @param action
    * @param subRoute
    * @return
    */
  private def casefileContentRoute(action: String, subRoute: (PlatformUser, Value[_], String, org.cafienne.cmmn.instance.casefile.Path) => Route): Route = {
    casefileRoute(action, (platformUser, caseInstanceId, path) => {
      entity(as[Value[_]]) { json => subRoute(platformUser, json, caseInstanceId, path) }
    })
  }

  /**
    * Run the action (replace, update, create, delete) for the user on the case instance with the path.
    *
    * @param action
    * @param subRoute
    * @return
    */
  private def casefileRoute(action: String, subRoute: (PlatformUser, String, org.cafienne.cmmn.instance.casefile.Path) => Route): Route = {
    validUser { platformUser =>
      pathPrefix(Segment / "casefile" / action) { caseInstanceId =>
        withCaseFilePath(path => subRoute(platformUser, caseInstanceId, path))
      }
    }
  }

  /**
    * Checks if the path ends or has more elements left, and returns any remains into a Cafienne Path object.
    * Empty path (either with or without slash results in an empty path object, referring to top level CaseFile)
    *
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
