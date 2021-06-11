package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import ch.megard.akka.http.cors.scaladsl.model.HttpHeaderRange
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.command.response.ModelResponse
import org.cafienne.json.Value
import org.cafienne.json.CafienneJson
import org.cafienne.service.api.Headers
import org.cafienne.system.CaseSystem

import scala.collection.immutable.Seq

/**
  * Base class for Case Service APIs. All cors enabled
  */
trait CaseServiceRoute extends LazyLogging {
  implicit val caseSystem: CaseSystem

  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  val corsSettings = CorsSettings.defaultSettings
    .withAllowedHeaders(HttpHeaderRange("Authorization", "Content-Type", "X-Requested-With", Headers.CASE_LAST_MODIFIED, Headers.TENANT_LAST_MODIFIED, "accept", "origin"))
    .withAllowedMethods(Seq(GET, POST, HEAD, OPTIONS, PUT, DELETE))
    .withExposedHeaders(Seq(Headers.CASE_LAST_MODIFIED, Headers.TENANT_LAST_MODIFIED))
    .withMaxAge(Some(200L)
    )

  val rejectionHandler = corsRejectionHandler withFallback requestServiceRejectionHandler
  val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

  val route: Route = handleErrors {
    extractExecutionContext { implicit executor =>
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
    }
  }

  def requestServiceRejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(errorMessage, e) =>
          extractUri { uri =>
            logger.debug(s"Exception of type ${e.getClass.getName} occurred in handling HTTP request ${uri.path} - $errorMessage")
            complete(StatusCodes.BadRequest, s"The request content was malformed:\n$errorMessage")
          }
      }
      .handle {
        case AuthorizationFailedRejection => complete(StatusCodes.Forbidden)
      }
      .result()

  def exceptionHandler = ExceptionHandler {
    case exception: Throwable => defaultExceptionHandler(exception)
  }

  def defaultExceptionHandler(t: Throwable): Route = {
    t match {
      case h: UnhealthyCaseSystem => complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = h.getLocalizedMessage))
      case _ => extractUri { uri =>
        extractMethod { method =>
          // Depending on debug logging - either print full exception or only headline
          if (logger.underlying.isDebugEnabled()) {
            logger.debug(s"Bumped into an exception in ${this.getClass().getSimpleName} on ${method.name} $uri", t)
          } else if (logger.underlying.isInfoEnabled()) {
            logger.info(s"Bumped into an exception in ${this.getClass().getSimpleName} on ${method.name} $uri:\n" + t)
          } else {
            logger.warn(s"Bumped into ${t.getClass.getName} in ${this.getClass().getSimpleName} on ${method.name} $uri - enable debug logging for stack trace; msg: " + t.getMessage)
          }
          complete(HttpResponse(StatusCodes.InternalServerError))
        }
      }
    }
  }

  def writeLastModifiedHeader(response: ModelResponse, headerName: String = Headers.CASE_LAST_MODIFIED): Directive0 = {
    respondWithHeader(RawHeader(headerName, response.lastModifiedContent.toString))
  }

  def completeCafienneJSONSeq(seq: Seq[CafienneJson]) = {
    completeJsonValue(Value.convert(seq.map(element => element.toValue)))
  }

  def completeJsonValue(v: Value[_]) = {
    complete(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, v.toString))
  }

  private var concatenatedSubRoutes: Route = null

  /**
    * Register a sub route; note: this requires an override of the prefix value as well,
    * and additionally the routes method should not be overridden
    * @param subRoute
    */
  def addSubRoute(subRoute: CaseServiceRoute) = {
    registerAPIRoute(subRoute)
    if (concatenatedSubRoutes == null) concatenatedSubRoutes = subRoute.routes
    else concatenatedSubRoutes = concat(concatenatedSubRoutes, subRoute.routes)
  }

  val prefix: String = "/"

  def routes: Route = {
    pathPrefix(prefix) {
      concatenatedSubRoutes
    }
  }

  def apiClasses(): Seq[Class[_]] = {
    swaggerClasses.to[Seq]
  }

  /**
    * Override this value and set to false if a route should not end up in Swagger
    */
  val addToSwaggerRoutes = true

  /**
    * Invoke this method to add it to the Swagger API classes
    * @param route
    * @return
    */
  def registerAPIRoute(route: CaseServiceRoute) = {
    if (route.addToSwaggerRoutes) {
      swaggerClasses += route.getClass
    }
  }

  private val swaggerClasses = scala.collection.mutable.Buffer[Class[_]]()
}
