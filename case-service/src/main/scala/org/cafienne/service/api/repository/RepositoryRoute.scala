/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.repository

import java.io.FileNotFoundException

import javax.ws.rs.{Consumes, GET, POST, Path, Produces}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import io.swagger.annotations._
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.cmmn.definition.{DefinitionsDocument, InvalidDefinitionException}
import org.cafienne.cmmn.instance.casefile.ValueMap
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.service.api.AuthenticatedRoute
import org.w3c.dom.Document
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.akka.actor.command.exception.MissingTenantException
import org.cafienne.identity.IdentityProvider

@Api(value = "repository", tags = Array("repository"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/repository")
class RepositoryRoute()(override implicit val userCache: IdentityProvider) extends AuthenticatedRoute {

  override def routes: Route =
    pathPrefix("repository") {
      loadModel ~
        listModels ~
        validateModel ~
        deployModel
    }

  @Path("load/{fileName}")
  @GET
  @Operation(
    summary = "Retrieve a case model",
    description = "Retrieve a case model by its filename",
    tags = Array("repository"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optional tenant in which to search for the model", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "fileName", description = "File name of the model", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Model found and returned", responseCode = "200"),
      new ApiResponse(description = "Model not found", responseCode = "404"),
      new ApiResponse(description = "Some error occured", responseCode = "500")
    )
  )
  @Produces(Array("application/xml"))
  def loadModel: Route = get {
    path("load" / Segment) { modelName =>
      validUser { user => {
        parameters('tenant ?) { optionalTenant =>
          try {
            val definitions = "/" + modelName + ".xml"
            val tenant = optionalTenant.getOrElse(CaseSystem.defaultTenant)
            logger.debug(s"Loading definitions from ${definitions}")
            val model = CaseSystem.DefinitionProvider.read(user.getTenantUser(tenant), definitions)
            complete(StatusCodes.OK, model.getDocument)
          }
          catch {
            case e: FileNotFoundException => complete(StatusCodes.NotFound)
            case i: InvalidDefinitionException => complete(StatusCodes.InternalServerError, i.toXML)
            case t: MissingTenantException => complete(StatusCodes.BadRequest, t.getMessage)
            case _: Exception => complete(StatusCodes.InternalServerError)
          }
        }
      }
      }
    }
  }

  @Path("list")
  @GET
  @Operation(
    summary = "Retrieve a list of deployed models",
    description = "Returns a list of deployed models",
    tags = Array("repository"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optional tenant in which to search for models", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
    ),
    responses = Array(
      new ApiResponse(description = "List of models returned", responseCode = "200"),
      new ApiResponse(description = "An error occurred", responseCode = "500")
    )
  )
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def listModels = get {
    path("list") {
      parameters('tenant ?) { optionalTenant =>
        validUser { user => {
          import scala.collection.JavaConverters._

          val tenant = optionalTenant.getOrElse(CaseSystem.defaultTenant)
          val models = new ValueMap // Resulting JSON structure: { 'models': [ {}, {}, {} ] }
          for (file <- CaseSystem.DefinitionProvider.list(user.getTenantUser(tenant)).asScala) {
            val dimensionsFile = file
            var description = "Description"
            try {
              val definitionsDocument = CaseSystem.DefinitionProvider.read(user.getTenantUser(tenant), file)
              description = definitionsDocument.getFirstCase().getDescription();
            } catch {
              case i: InvalidDefinitionException => description = i.toString
              case t: Throwable => description = "Could not read definition: " + t.getMessage
            }
            val model = new ValueMap("definitions", file, "dimensions", dimensionsFile, "description", description)
            models.withArray("models").add(model)
          }
          complete(StatusCodes.OK, models)
        }
        }
      }
    }
  }

  @Path("/validate")
  @POST
  @Operation(
    summary = "Validate a case model",
    description = "Validate a case model definitions file and return the validation errors, or OK",
    tags = Array("repository"),
    responses = Array(
      new ApiResponse(description = "Model validated", responseCode = "200"),
      new ApiResponse(description = "Model is invalid", responseCode = "500")
    )
  )
  @RequestBody(description = "Definitions file", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[String]))))
  @Consumes(Array("application/xml"))
  @Produces(Array("application/json"))
  def validateModel = post {
    path("validate") { // NOTE: This API is open, you do not need to have user credentials for it.
      entity(as[Document]) { payload =>
        try {
          // If we are able to instantiate a new DefinitionsDocument, then the content is valid, and we can return ok.
          new DefinitionsDocument(payload)
          complete(StatusCodes.OK)
        } catch {
          case idd: InvalidDefinitionException => complete(StatusCodes.BadRequest, idd.toJSON.toString)
          case other: Throwable => throw other
        }
      }
    }
  }

  @Path("{tenant}/deploy/{fileName}")
  @POST
  @Operation(
    summary = "Deploy a case model",
    description = "Deploy a case model definitions file to the deploy folder",
    tags = Array("repository"),
    parameters = Array(
      new Parameter(name = "tenant", description = "Optional tenant in which to deploy the model", in = ParameterIn.QUERY, schema = new Schema(implementation = classOf[String]), required = false),
      new Parameter(name = "fileName", description = "File name of the model without extension", in = ParameterIn.PATH, schema = new Schema(implementation = classOf[String]), required = true),
    ),
    responses = Array(
      new ApiResponse(description = "Model deployed/saved", responseCode = "201"),
      new ApiResponse(description = "Some error occured", responseCode = "500")
    )
  )
  @RequestBody(description = "Definitions XML file", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[String]))))
  @Consumes(Array("application/xml"))
  def deployModel = post {
    path("repository" / "deploy" / Segment) { modelName =>
      // For deploying files through the network, authorization is mandatory
      validUser { user => {
        parameters('tenant ?) { optionalTenant =>
          entity(as[Document]) { xmlDocument =>
            try {
              logger.debug(s"Deploying ${modelName}")
              val tenant = optionalTenant.getOrElse(CaseSystem.defaultTenant)
              val definitions = new DefinitionsDocument(xmlDocument)
              // Reaching this point means we have a valid definition, and can simply move on.
              CaseSystem.DefinitionProvider.write(user.getTenantUser(tenant), modelName + ".xml", definitions)
              complete(StatusCodes.Created)
            } catch {
              case idd: InvalidDefinitionException => complete(StatusCodes.InternalServerError, new Exception(idd.toJSON.toString))
              case other: Throwable => complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
      }
    }
  }
}