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

package org.cafienne.service.infrastructure.route

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.http.cors.scaladsl.model.HttpHeaderRange
import org.apache.pekko.http.cors.scaladsl.settings.CorsSettings
import org.apache.pekko.http.scaladsl.model.HttpMethods._
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server._
import org.cafienne.json.{CafienneJson, Value}
import org.cafienne.persistence.infrastructure.lastmodified.Headers
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.system.CaseSystem
import org.cafienne.util.XMLHelper
import org.w3c.dom.Node

/**
  * Base class for Case Service APIs. All cors enabled
  */
trait CaseServiceRoute extends LazyLogging {
  val httpService: CaseEngineHttpServer
  val caseSystem: CaseSystem = httpService.caseSystem

  import org.apache.pekko.http.cors.scaladsl.CorsDirectives._

  private val corsSettings = CorsSettings.default(caseSystem.system)
    .withAllowedHeaders(HttpHeaderRange("Authorization", "Content-Type", "X-Requested-With", Headers.CASE_LAST_MODIFIED, Headers.TENANT_LAST_MODIFIED, "accept", "origin"))
    .withAllowedMethods(Seq(GET, POST, HEAD, OPTIONS, PUT, DELETE))
    .withExposedHeaders(Seq(Headers.CASE_LAST_MODIFIED, Headers.TENANT_LAST_MODIFIED))
    .withMaxAge(Some(200L)
    )

  private val rejectionHandler = corsRejectionHandler withFallback requestServiceRejectionHandler
  private val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)
 
  val route: Route = handleErrors {
    //extractExecutionContext { implicit executor =>
    cors(corsSettings) {
      handleErrors { req =>
        //          println("Asking "+req.request.uri)
        routes(req)
        //            .map(resp => {
        //              println("Responding to " + req.request.uri + ": " + resp)
        //              println("" + resp)
        //              resp
        //            })
      }
    }
    // }
  }

  // Give more information back to client on various types of rejections
  //  Also do some server side logging when in debug mode
  def requestServiceRejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(errorMessage, e) =>
          extractRequest { request =>
            logger.debug(s"HTTP request ${request.method.value} ${request.uri} has malformed content (${e.getClass.getName} - '$errorMessage')")
            val cause = if (e.getCause != null) e.getCause.getMessage else e.getMessage
            complete(StatusCodes.BadRequest, "The request content was malformed: " + cause)
          }
        case a: UnsupportedRequestContentTypeRejection =>
          extractRequest { request =>
            logger.debug(s"HTTP request ${request.method.value} ${request.uri} comes with unsupported content type '${a.contentType.getOrElse("")}'; it needs one of ${a.supported}")
            complete(StatusCodes.BadRequest, s"The request content type ${a.contentType} is not supported, provide one of ${a.supported}")
          }
      }
      .result()

  def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case exception: Throwable => defaultExceptionHandler(exception)
  }

  def defaultExceptionHandler(t: Throwable): Route = {
    t match {
      case h: UnhealthyCaseSystem => complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = h.getLocalizedMessage))
      case _ => extractUri { uri =>
        extractMethod { method =>
          // Depending on debug logging - either print full exception or only headline
          if (logger.underlying.isDebugEnabled()) {
            logger.debug(s"Bumped into an exception in ${this.getClass.getSimpleName} on ${method.name} $uri", t)
          } else if (logger.underlying.isInfoEnabled()) {
            logger.info(s"Bumped into an exception in ${this.getClass.getSimpleName} on ${method.name} $uri:\n" + t)
          } else {
            logger.warn(s"Bumped into ${t.getClass.getName} in ${this.getClass.getSimpleName} on ${method.name} $uri - enable debug logging for stack trace; msg: " + t.getMessage)
          }
          complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  def completeJson(seq: Seq[CafienneJson]): Route = completeJson(StatusCodes.OK, Value.convert(seq.map(_.toValue)))

  def completeJson(c: CafienneJson): Route = completeJson(StatusCodes.OK, c.toValue)

  def completeJson(v: Value[_]): Route = completeJson(StatusCodes.OK, v)

  def completeJson(statusCode: StatusCode, v: Value[_]) = complete(statusCode, HttpEntity(ContentTypes.`application/json`, v.toString))

  def completeXML(n: Node, statusCode: StatusCode = StatusCodes.OK): Route = complete(statusCode, HttpEntity(ContentTypes.`text/xml(UTF-8)`, XMLHelper.printXMLNode(n)))

  private var concatenatedSubRoutes: Option[Route] = None

  /**
    * Register a sub route; note: this supports an optional override of the prefix value as well.
    */
  def addSubRoute(subRoute: CaseServiceRoute): Unit = {
    def subRouteCollection: Route = {
      // If the sub route has a non-standard prefix defined, let's add it
      if (subRoute.prefix != DEFAULT_PREFIX) {
        pathPrefix(subRoute.prefix)(subRoute.routes)
      } else {
        subRoute.routes
      }
    }

    registerAPIRoute(subRoute)
    concatenatedSubRoutes = concatenatedSubRoutes.fold(Some(subRouteCollection))(route => Some(concat(route, subRouteCollection)))
  }

  private val DEFAULT_PREFIX = "/"

  val prefix: String = DEFAULT_PREFIX

  def routes: Route = {
    pathPrefix(prefix) {
      concatenatedSubRoutes.get
    }
  }

  def apiClasses(): Seq[Class[_]] = {
    swaggerClasses.toSeq
  }

  /**
    * Override this value and set to false if a route should not end up in Swagger
    */
  val addToSwaggerRoutes = true

  /**
    * Invoke this method to add it to the Swagger API classes
    *
    * @param route
    * @return
    */
  def registerAPIRoute(route: CaseServiceRoute): Unit = {
    if (route.addToSwaggerRoutes) {
      swaggerClasses += route.getClass
    }
  }

  private val swaggerClasses = scala.collection.mutable.Buffer[Class[_]]()
}
