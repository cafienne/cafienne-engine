package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import ch.megard.akka.http.cors.scaladsl.model.HttpHeaderRange
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.service.api

import scala.collection.immutable.Seq

/**
  * Base class for Case Service APIs. All cors enabled
  */
trait CaseServiceRoute extends LazyLogging {

  import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

  val corsSettings = CorsSettings.defaultSettings
    .withAllowedHeaders(HttpHeaderRange("Authorization", "Content-Type", "X-Requested-With", api.CASE_LAST_MODIFIED, "accept", "origin"))
    .withAllowedMethods(Seq(GET, POST, HEAD, OPTIONS, PUT, DELETE))
    .withMaxAge(Some(200L)
    )

  val rejectionHandler = corsRejectionHandler withFallback requestServiceRejectionHandler
  val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

  val route: Route = handleErrors {
    extractExecutionContext { implicit executor =>
      cors(corsSettings) {
        handleErrors { req => {
          //          println("Asking "+req.request.uri)
          routes(req).map(resp => {
            //            println("Responding to "+req.request.uri+": "+resp)
            //            println(""+resp)
            resp
          })
        }
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
            logger.debug("Exception of type " + e.getClass.getName + " occured in handling HTTP request " + uri.path + " - " + errorMessage)
            complete(StatusCodes.BadRequest, "The request content was malformed:\n" + errorMessage)
          }
      }
      .handle {
        case AuthorizationFailedRejection ⇒ complete(StatusCodes.Forbidden)
      }
      .result()

  def exceptionHandler = ExceptionHandler {
    case exception: Throwable => {
      extractUri { uri =>
        defaultExceptionHandler(exception, uri)
      }
    }
  }

  def defaultExceptionHandler(t: Throwable, uri: Uri) = {
    // Simply print headline of the exception
    logger.info("Bumped into an exception in " + this.getClass().getSimpleName() + " on uri " + uri + ":\n" + t)
    logger.debug("Bumped into an exception in " + this.getClass().getSimpleName() + " on uri " + uri + ":\n" + t, t)
    complete(HttpResponse(StatusCodes.InternalServerError))
  }

  def routes: Route

  /**
    * Override this method in your route to expose the Swagger API classes
    *
    * @return
    */
  def apiClasses(): Seq[Class[_]] = {
    Seq()
  }
}
