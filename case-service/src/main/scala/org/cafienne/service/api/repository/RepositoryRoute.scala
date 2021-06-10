/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.repository

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.CaseSystem
import org.cafienne.actormodel.command.exception.{AuthorizationException, MissingTenantException}
import org.cafienne.actormodel.config.Cafienne
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.json.ValueMap

import javax.ws.rs._
import org.cafienne.actormodel.command.exception.MissingTenantException
import org.cafienne.cmmn.definition.{DefinitionsDocument, InvalidDefinitionException}
import org.cafienne.cmmn.repository.{MissingDefinitionException, WriteDefinitionException}
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.infrastructure.akka.http.route.AuthenticatedRoute
import org.w3c.dom.Document

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/repository")
class RepositoryRoute()(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends AuthenticatedRoute {

  override def routes: Route = pathPrefix("repository") { concat(loadModel, listModels, validateModel, deployModel) }

  registerAPIRoute(this)

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
      userWithTenant { (platformUser, tenant) => {
        try {
          logger.debug(s"Loading definitions '$modelName' from tenant '$tenant'")
          val model = Cafienne.config.repository.DefinitionProvider.read(platformUser, tenant, modelName)
          complete(StatusCodes.OK, model.getDocument)
        }
        catch {
          case m: MissingDefinitionException => complete(StatusCodes.NotFound, "A model with name " + modelName + " cannot be found")
          case i: InvalidDefinitionException => complete(StatusCodes.InternalServerError, i.toXML)
          case t: MissingTenantException => complete(StatusCodes.BadRequest, t.getMessage)
          case other: Exception => {
            logger.error("Unexpected loading failure", other)
            complete(StatusCodes.InternalServerError)
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
      userWithTenant { (platformUser, tenant) => {
        import scala.collection.JavaConverters._

        val models = new ValueMap // Resulting JSON structure: { 'models': [ {}, {}, {} ] }
        for (file <- Cafienne.config.repository.DefinitionProvider.list(platformUser, tenant).asScala) {
          var description = "Description"
          try {
            val definitionsDocument = Cafienne.config.repository.DefinitionProvider.read(platformUser, tenant, file)
            description = definitionsDocument.getFirstCase().documentation.text
          } catch {
            case i: InvalidDefinitionException => description = i.toString
            case t: Throwable => description = "Could not read definition: " + t.getMessage
          }
          val model = new ValueMap("definitions", file, "description", description)
          models.withArray("models").add(model)
        }
        completeJsonValue(models)
      }
      }
    }
  }

  @Path("validate")
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

  @Path("deploy/{fileName}")
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
    path("deploy" / Segment) { modelName =>
      // For deploying files through the network, authorization is mandatory
      userWithTenant { (platformUser, tenant) => {
        entity(as[Document]) { xmlDocument =>
          try {
            logger.debug(s"Deploying '$modelName' to tenant '$tenant'")
            // First check if we can parse the definition
            val definitions = new DefinitionsDocument(xmlDocument)

            val tenantUser = platformUser.getTenantUser(tenant)
            tenantUser.isOwner match {
              case false => complete(StatusCodes.Unauthorized, "User '" + platformUser.userId + "' does not have the privileges to deploy a definition")
              case true => {
                Cafienne.config.repository.DefinitionProvider.write(platformUser, tenant, modelName, definitions)
                complete(StatusCodes.NoContent)
              }
            }
          } catch {
            case idd: InvalidDefinitionException => {
              logger.debug("Deployment failure", idd)
              complete(StatusCodes.BadRequest, "Cannot deploy " + modelName + ": definition is invalid.")
            }
            case failure: WriteDefinitionException => {
              logger.debug("Deployment failure", failure)
              complete(StatusCodes.BadRequest, failure.getLocalizedMessage)
            }
            case failure: AuthorizationException => {
              logger.debug("Deployment failure", failure)
              complete(StatusCodes.Unauthorized, failure.getLocalizedMessage)
            }
            case other: Throwable => {
              logger.debug("Unexpected deployment failure", other)
              complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
      }
    }
  }

  /**
    * Reads validUser and resolves a tenant, based on either the optional tenant passed as a parameter,
    * or the default tenant of the user
    *
    * @param subRoute
    * @return
    */
  private def userWithTenant(subRoute: (PlatformUser, String) => Route): Route = {
    validUser { platformUser =>
      parameters('tenant ?) { optionalTenant =>
        val tenant = platformUser.resolveTenant(optionalTenant)
        subRoute(platformUser, tenant)
      }
    }
  }
}