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

package org.cafienne.infrastructure.akkahttp.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import org.cafienne.actormodel.exception.{AuthorizationException, InvalidCommandException}
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.actormodel.response.ActorLastModified
import org.cafienne.authentication.{AuthenticatedUser, AuthenticationException, CannotReachIDPException}
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.akkahttp.authentication.{AuthenticationDirectives, IdentityProvider}
import org.cafienne.querydb.materializer.tenant.TenantReader
import org.cafienne.querydb.query.exception.SearchFailure
import org.cafienne.service.akkahttp.Headers
import org.cafienne.system.health.HealthMonitor

import scala.concurrent.ExecutionContext


/**
  * Base for enabling authentication
  */
trait AuthenticatedRoute extends CaseServiceRoute with AuthenticationDirectives {

  override val userCache: IdentityProvider = caseSystem.userCache
  override implicit val ex: ExecutionContext = caseSystem.system.dispatcher

  override def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: CannotReachIDPException => handleIDPException(e)
    case s: AuthenticationException => handleAuthenticationException(s)
    case a: AuthorizationException => handleAuthorizationException(a)
    case i: InvalidCommandException => handleInvalidCommandException(i)
    case s: SearchFailure => complete(StatusCodes.NotFound, s.getMessage)
    case s: SecurityException => handleAuthorizationException(s) // Pretty weird, as our code does not throw it; log it similar to Authorizaton issues
    case other => defaultExceptionHandler(other) // All other exceptions just handle the default way from CaseServiceRoute
  }

  private def handleInvalidCommandException(i: InvalidCommandException): Route = {
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

  private def handleIDPException(e: CannotReachIDPException): Route = {
    logger.error("Service cannot validate security tokens, because IDP is not reachable")
    complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = e.getMessage)).andThen(g => {
      // TODO: this probably should be checked upon system startup in the first place
      //              System.err.println("CANNOT REACH IDP, downing the system")
      //              System.exit(-1)
      //              CaseSystem.system.terminate()
      g
    })
  }

  private def handleAuthenticationException(s: AuthenticationException): Route = {
    complete(HttpResponse(StatusCodes.Unauthorized, entity = s.getMessage()))
  }

  private def handleAuthorizationException(s: Exception): Route = {
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

  def authenticatedUser(subRoute: AuthenticatedUser => Route): Route = {
    authenticatedUser() { user => {
        caseSystemMustBeHealthy()
        optionalHeaderValueByName(Headers.TENANT_LAST_MODIFIED) {
          case Some(s) =>
            // Wait for the TenantReader to be informed about the tenant-last-modified timestamp
            onComplete(TenantReader.lastModifiedRegistration.waitFor(new ActorLastModified(s)).future) {
              _ => subRoute(user)
            }
          // Nothing to wait for, just continue and execute the query straight on
          case None => subRoute(user)
        }
      }
    }
  }

  def validUser(subRoute: PlatformUser => Route): Route = {
    optionalHeaderValueByName(Headers.TENANT_LAST_MODIFIED) { tlm =>
      platformUser(tlm) { platformUser =>
        caseSystemMustBeHealthy()
        subRoute(platformUser)
      }
    }
  }

  def caseSystemMustBeHealthy(): Unit = {
    if (!HealthMonitor.ok()) {
      throw new UnhealthyCaseSystem("Refusing request, because Case System is not healthy")
    }
  }
}