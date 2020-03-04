package org.cafienne.infrastructure.akka.http.route

import java.net.URL

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractExecutionContext, extractUri}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import com.nimbusds.jose.jwk.source.{JWKSource, RemoteJWKSet}
import com.nimbusds.jose.proc.SecurityContext
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.authentication.{AuthenticationDirectives, CannotReachIDPException}

import scala.concurrent.ExecutionContext


/**
  * Base for enabling authentication
  */
trait AuthenticatedRoute extends CaseServiceRoute {

  implicit val userCache : IdentityProvider
  val uc = userCache

  object OIDCAuthentication extends Directives with AuthenticationDirectives {
    protected val keySource: JWKSource[SecurityContext] = new RemoteJWKSet(new URL(CaseSystem.config.OIDC.keysUrl))
    protected val issuer                                = CaseSystem.config.OIDC.issuer
    override protected val userCache: IdentityProvider = uc
    override protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  }

  override def caseSystemMustBeHealthy = {
    if (! CaseSystem.health.ok) {
      throw new UnhealthyCaseSystem("Refusing request, because Case System is not healthy")
    }
  }

  override def exceptionHandler = ExceptionHandler {
    case h: UnhealthyCaseSystem => complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = h.getLocalizedMessage))
    case s: SecurityException =>
      extractUri { uri =>
        logger.warn(s"Request to $uri has a security issue: " + s)
        if (logger.underlying.isInfoEnabled()) {
          logger.info("", s)
        } else {
          logger.warn("Enable info logging to see more details on the security issue")
        }
        s match {
          case e: CannotReachIDPException => {
            logger.error("Service cannot validate security tokens, because IDP is not reachable")
            complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = e.getMessage)).andThen(g => {
              // TODO: this probably should be checked upon system startup in the first place
              //              System.err.println("CANNOT REACH IDP, downing the system")
              //              System.exit(-1)
              //              CaseSystem.system.terminate()
              g
            })
          }
          case _ => complete(HttpResponse(StatusCodes.Unauthorized, entity = s.getMessage()))

        }
      }
    case other => {
      extractUri { uri =>
        defaultExceptionHandler(other, uri)
      }
    }
  }

  def validUser(subRoute: PlatformUser => Route): Route = {
    caseSystemMustBeHealthy
    OIDCAuthentication.user { usr => subRoute(usr)  }
  }

  // TODO: this is a temporary switch to enable IDE's debugger to show events
  @Deprecated // but no alternative yet...
  def optionalUser(subRoute: PlatformUser => Route) : Route = {
    if (CaseSystem.config.developerRouteOpen) {
      subRoute(null)
    } else {
      validUser(subRoute)
    }
  }


}