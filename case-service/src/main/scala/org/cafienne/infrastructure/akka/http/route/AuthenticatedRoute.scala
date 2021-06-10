package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractUri, _}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import com.nimbusds.jose.jwk.source.{JWKSource, RemoteJWKSet}
import com.nimbusds.jose.proc.SecurityContext
import org.cafienne.actormodel.command.exception.{AuthorizationException, InvalidCommandException}
import org.cafienne.system.health.HealthMonitor
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.actormodel.command.exception.InvalidCommandException
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.akka.http.authentication.{AuthenticationDirectives, AuthenticationException, CannotReachIDPException}
import org.cafienne.service.api.Headers

import java.net.URL
import scala.concurrent.ExecutionContext


/**
  * Base for enabling authentication
  */
trait AuthenticatedRoute extends CaseServiceRoute {

  implicit val userCache: IdentityProvider
  val uc = userCache

  override def exceptionHandler = ExceptionHandler {
    case e: CannotReachIDPException => handleIDPException(e)
    case s: AuthenticationException => handleAuthenticationException(s)
    case a: AuthorizationException => handleAuthorizationException(a)
    case i: InvalidCommandException => handleInvalidCommandException(i)
    case s: SecurityException => handleAuthorizationException(s) // Pretty weird, as our code does not throw it; log it similar to Authorizaton issues
    case other => defaultExceptionHandler(other) // All other exceptions just handle the default way from CaseServiceRoute
  }

  private def handleInvalidCommandException(i: InvalidCommandException) = {
    if (logger.underlying.isDebugEnabled) {
      extractUri { uri =>
        extractMethod { method =>
          logger.debug(s"Invalid command on ${method.value} $uri", i)
          complete(StatusCodes.BadRequest, i.getMessage)
        }
      }
    } else {
      complete(StatusCodes.BadRequest, i.getMessage)
    }
  }

  private def handleIDPException(e: CannotReachIDPException) = {
    logger.error("Service cannot validate security tokens, because IDP is not reachable")
    complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = e.getMessage)).andThen(g => {
      // TODO: this probably should be checked upon system startup in the first place
      //              System.err.println("CANNOT REACH IDP, downing the system")
      //              System.exit(-1)
      //              CaseSystem.system.terminate()
      g
    })
  }

  private def handleAuthenticationException(s: AuthenticationException) = {
    complete(HttpResponse(StatusCodes.Unauthorized, entity = s.getMessage()))
  }

  private def handleAuthorizationException(s: Exception) = {
    extractMethod { method =>
      extractUri { uri =>
        if (logger.underlying.isInfoEnabled()) {
          logger.info(s"Authorization issue in request ${method.name} $uri ", s)
        } else {
          logger.warn(s"Authorization issue in request ${method.name} $uri (enable info logging for stack trace): " + s.getMessage)
        }
        complete(HttpResponse(StatusCodes.Unauthorized, entity = s.getMessage()))
      }
    }
  }

  // TODO: this is a temporary switch to enable IDE's debugger to show events
  @Deprecated // but no alternative yet...
  def optionalUser(subRoute: PlatformUser => Route): Route = {
    if (Cafienne.config.developerRouteOpen) {
      subRoute(null)
    } else {
      validUser(subRoute)
    }
  }

  def validUser(subRoute: PlatformUser => Route): Route = {
    optionalHeaderValueByName(Headers.TENANT_LAST_MODIFIED) { tlm =>
      OIDCAuthentication.user(tlm) { platformUser =>
        caseSystemMustBeHealthy
        subRoute(platformUser)
      }
    }
  }

  def validOwner(subRoute: PlatformUser => Route): Route = {
    validUser { platformUser =>
      platformUser.isPlatformOwner match {
        case true => subRoute(platformUser)
        case false => complete(StatusCodes.Unauthorized, "Only platform owners can access this route")
      }
    }
  }

  def caseSystemMustBeHealthy = {
    if (!HealthMonitor.ok) {
      throw new UnhealthyCaseSystem("Refusing request, because Case System is not healthy")
    }
  }

  object OIDCAuthentication extends Directives with AuthenticationDirectives {
    override protected val userCache: IdentityProvider = uc
    protected val keySource: JWKSource[SecurityContext] = new RemoteJWKSet(new URL(Cafienne.config.OIDC.keysUrl))
    protected val issuer = Cafienne.config.OIDC.issuer
    override protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  }
}