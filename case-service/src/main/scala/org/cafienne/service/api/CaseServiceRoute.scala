package org.cafienne.service.api

import java.net.URL

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import ch.megard.akka.http.cors.scaladsl.model.HttpHeaderRange
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.nimbusds.jose.jwk.source.{JWKSource, RemoteJWKSet}
import com.nimbusds.jose.proc.SecurityContext
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.authentication.AuthenticationDirectives
import org.cafienne.service.api

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

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

  def requestServiceRejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(errorMessage, e) =>
          extractUri { uri  =>
              logger.debug("Exception of type "+e.getClass.getName+" occured in handling HTTP request "+uri.path+" - " + errorMessage)
              complete(StatusCodes.BadRequest, "The request content was malformed:\n" + errorMessage)
          }
      }
      .handle {
        case AuthorizationFailedRejection ⇒ complete(StatusCodes.Forbidden)
      }
      .result()

  val rejectionHandler = corsRejectionHandler withFallback requestServiceRejectionHandler

  def exceptionHandler = ExceptionHandler {
    case exception: Throwable => {
      extractUri { uri =>
        defaultExceptionHandler(exception, uri)
      }
    }
  }

  def defaultExceptionHandler(t: Throwable, uri: Uri) = {
    // Simply print headline of the exception
    logger.info("Bumped into an exception in "+this.getClass().getSimpleName()+" on uri "+uri+":\n" + t)
    logger.debug("Bumped into an exception in "+this.getClass().getSimpleName()+" on uri "+uri+":\n" + t, t)
    complete(HttpResponse(StatusCodes.InternalServerError))
  }

  val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

  val route : Route = handleErrors {
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
  def routes : Route
}

/**
  * Base for enabling authentication
  */
trait AuthenticatedRoute extends CaseServiceRoute {

  def userCache : IdentityProvider
  val uc = userCache

  object OIDCAuthentication extends Directives with AuthenticationDirectives {
    protected val keySource: JWKSource[SecurityContext] = new RemoteJWKSet(new URL(CaseSystem.OIDC.keysUrl))
    protected val issuer                                = CaseSystem.OIDC.issuer
    override protected val userCache: IdentityProvider = uc
    override protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  }

  override def exceptionHandler = ExceptionHandler {
    case s: SecurityException =>
      extractUri { uri =>
        logger.info(s"Request to $uri could not be handled normally " + s)
        complete(HttpResponse(StatusCodes.Unauthorized, entity = s.getMessage()))
      }
    case other => {
      extractUri { uri =>
        defaultExceptionHandler(other, uri)
      }
    }
  }

  def validUser(subRoute: PlatformUser => Route): Route = OIDCAuthentication.user { usr => subRoute(usr)  }

  // TODO: this is a temporary switch to enable IDE's debugger to show events
  def optionalUser(subRoute: PlatformUser => Route) : Route = {
//    println("Allowing with optional user is: " + CaseSystem.developerRouteOpen)
    if (CaseSystem.developerRouteOpen) {
//      println("Going with optional user None")
      subRoute(null)
    } else {
//      println("Anonymous entry is not an option")
      validUser(subRoute)
    }
  }
}