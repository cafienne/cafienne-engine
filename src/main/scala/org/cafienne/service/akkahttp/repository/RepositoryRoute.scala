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

package org.cafienne.service.akkahttp.repository

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.actormodel.exception.{AuthorizationException, MissingTenantException}
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.cmmn.definition.{DefinitionsDocument, InvalidDefinitionException}
import org.cafienne.cmmn.repository.{MissingDefinitionException, WriteDefinitionException}
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.akkahttp.HttpXmlReader._
import org.cafienne.infrastructure.akkahttp.route.{AuthenticatedRoute, TenantValidator}
import org.cafienne.json.ValueMap
import org.cafienne.service.akkahttp.cases.CaseAPIFormat.CaseDefinitionFormat
import org.cafienne.service.akkahttp.repository.RepositoryAPIFormat.ModelListResponseFormat
import org.cafienne.system.CaseSystem
import org.w3c.dom.Document

import javax.ws.rs._
import scala.concurrent.ExecutionContext

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/repository")
class RepositoryRoute(override val caseSystem: CaseSystem) extends AuthenticatedRoute with TenantValidator {
  implicit val ec: ExecutionContext = caseSystem.system.dispatcher

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
      new ApiResponse(description = "Model found and returned", responseCode = "200", content = Array(new Content(mediaType = "text/xml", schema = new Schema(implementation = classOf[CaseDefinitionFormat])))),
      new ApiResponse(description = "Model not found", responseCode = "404"),
    )
  )
  @Produces(Array("application/xml"))
  def loadModel: Route = get {
    path("load" / Segment) { modelName =>
      userWithTenant { (platformUser, tenant) => {
        try {
          logger.debug(s"Loading definitions '$modelName' from tenant '$tenant'")
          val model = Cafienne.config.repository.DefinitionProvider.read(platformUser, tenant, modelName)
          completeXML(model.getDocument)
        }
        catch {
          case m: MissingDefinitionException => complete(StatusCodes.NotFound, "A model with name " + modelName + " cannot be found")
          case i: InvalidDefinitionException => completeXML(i.toXML, StatusCodes.InternalServerError)
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
      new ApiResponse(description = "List of models returned", responseCode = "200", content = Array(new Content(schema = new Schema(implementation = classOf[ModelListResponseFormat])))),
    )
  )
  @Consumes(Array("application/json"))
  @Produces(Array("application/json"))
  def listModels: Route = get {
    path("list") {
      userWithTenant { (platformUser, tenant) => {
        import scala.jdk.CollectionConverters._

        val models = new ValueMap()
        models.withArray("models") // Resulting JSON structure: { 'models': [ {}, {}, {} ] }
        for (file <- Cafienne.config.repository.DefinitionProvider.list(platformUser, tenant).asScala) {
          var description = "Description"
          try {
            val definitionsDocument = Cafienne.config.repository.DefinitionProvider.read(platformUser, tenant, file)
            description = definitionsDocument.getFirstCase.documentation.text
          } catch {
            case i: InvalidDefinitionException => description = i.toString
            case t: Throwable => description = "Could not read definition: " + t.getMessage
          }
          val model = new ValueMap("definitions", file, "description", description)
          models.withArray("models").add(model)
        }
        completeJson(models)
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
      new ApiResponse(description = "Model is invalid", responseCode = "400")
    )
  )
  @RequestBody(description = "Definitions file", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[String]))))
  @Consumes(Array("application/xml"))
  @Produces(Array("application/json"))
  def validateModel: Route = post {
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
      new ApiResponse(description = "Model deployed/saved", responseCode = "204"),
      new ApiResponse(description = "Invalid model or failure while writing", responseCode = "400"),
      new ApiResponse(description = "Not Authorized", responseCode = "401"),
      new ApiResponse(description = "Some unexpected error occured", responseCode = "500")
    )
  )
  @RequestBody(description = "Definitions XML file", required = true, content = Array(new Content(schema = new Schema(implementation = classOf[String]))))
  @Consumes(Array("application/xml"))
  def deployModel: Route = post {
    path("deploy" / Remaining ) { modelName =>
      // For deploying files through the network, authorization is mandatory
      userWithTenant { (platformUser, tenant) => {
        entity(as[Document]) { xmlDocument =>
          try {
            logger.debug(s"Deploying '$modelName' to tenant '$tenant'")
            // First check if we can parse the definition
            val definitions = new DefinitionsDocument(xmlDocument)

            val tenantUser = platformUser.getTenantUser(tenant)
            if (tenantUser.isOwner) {
              Cafienne.config.repository.DefinitionProvider.write(platformUser, tenant, modelName, definitions)
              complete(StatusCodes.NoContent)
            } else {
              complete(StatusCodes.Unauthorized, "User '" + platformUser.id + "' does not have the privileges to deploy a definition")
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
      parameters("tenant".?) { optionalTenant =>
        val tenant = platformUser.resolveTenant(optionalTenant)
        validateTenant(tenant, subRoute(platformUser, tenant))
      }
    }
  }
}