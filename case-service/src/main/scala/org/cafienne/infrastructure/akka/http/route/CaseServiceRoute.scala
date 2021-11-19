package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import ch.megard.akka.http.cors.scaladsl.model.HttpHeaderRange
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.response.ModelResponse
import org.cafienne.json.{CafienneJson, Value}
import org.cafienne.service.api.Headers
import org.cafienne.system.CaseSystem

import scala.collection.immutable.Seq

/**
  * Base class for Case Service APIs. All cors enabled
  */
trait CaseServiceRoute extends LazyLogging {
  implicit val caseSystem: CaseSystem

  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  private val corsSettings = CorsSettings.defaultSettings
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

  def writeLastModifiedHeader(response: ModelResponse, headerName: String = Headers.CASE_LAST_MODIFIED): Directive0 = {
    val lm = response.lastModifiedContent().toString
    if (lm != null) {
      respondWithHeader(RawHeader(headerName, response.lastModifiedContent.toString))
    } else {
      respondWithHeaders(Seq())
    }
  }

  def completeCafienneJSONSeq(seq: Seq[CafienneJson]): Route = {
    completeJsonValue(Value.convert(seq.map(element => element.toValue)))
  }

  def completeJsonValue(v: Value[_]): Route = {
    complete(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, v.toString))
  }

  private var concatenatedSubRoutes: Option[Route] = None

  /**
    * Register a sub route; note: this requires an override of the prefix value as well,
    * and additionally the routes method should not be overridden
    * @param subRoute
    */
  def addSubRoute(subRoute: CaseServiceRoute): Unit = {
    registerAPIRoute(subRoute)
    concatenatedSubRoutes = concatenatedSubRoutes.fold(Some(subRoute.routes))(route => Some(concat(route, subRoute.routes)))
  }

  val prefix: String = "/"

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
