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

package org.cafienne.service.infrastructure.authentication

import com.nimbusds.jose.RemoteKeySourceException
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jwt.proc.BadJWTException
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.IdentityRegistration
import org.cafienne.service.infrastructure.configuration.OIDCConfiguration
import org.cafienne.system.health.HealthMonitor

import java.text.ParseException
import scala.concurrent.{ExecutionContext, Future}

class TokenVerifier(val userRegistration: IdentityRegistration, val config: OIDCConfiguration)(implicit ec: ExecutionContext) extends LazyLogging {

  private val userReader = new AuthenticatedUserReader(this)

  def convertToAuthenticatedUser(token: String): Future[AuthenticatedUser] = Future {
    try {
      // First create a TokenContext. This context has information needed for the creation of the AuthenticatedUser
      val context = userReader.createUserContext(token)
      userReader.process(token, context)
      // Reaching this point means no exceptions happened on contacting the IDP
      HealthMonitor.idp.isOK()
      userRegistration.cacheUserToken(context.authenticatedUser, token)
      // Return the authenticated user that was created based on the token
      context.authenticatedUser
    } catch {
      case rp: RemoteKeySourceException =>
        // TODO: this should return a HTTP code 503 Service Unavailable!
        logger.error("Failure in contacting IDP. Check IDP configuration settings of the case engine.", rp)
        val failure = new CannotReachIDPException("Cannot reach the IDP to validate credentials", rp)
        HealthMonitor.idp.hasFailed(failure)
        throw failure
      case other: Throwable =>
        HealthMonitor.idp.isOK()
        other match {
          case nje: BadJWTException =>
            // These Exceptions are thrown from the DefaultJWTClaimsVerifier. We can only use the message to distinguish
            //  one from the other, and that message is sometimes not quite readable

            // From checking the "exp" claim
            if (nje.getMessage == "Expired JWT") {
              throw new TokenVerificationException("JWT Token failure: token is expired")
            }

            // Claims must have proper format, if not, then a parse exception is thrown. This can be nested.
            if (nje.getCause.isInstanceOf[ParseException]) {
              throw new TokenVerificationException("Token parse failure: " + nje.getCause.getLocalizedMessage)
            }

            throw TokenVerificationException("Invalid token: " + nje.getLocalizedMessage)
          case e: BadJOSEException =>
            // This captures both JWS and JWE exceptions. These are really technical, and logger.debug must be enabled to understand them
            logger.debug("Encountered JWT issues", e)
            throw TokenVerificationException("Token cannot be verified: " + e.getLocalizedMessage)
          case e: ParseException =>
            throw TokenVerificationException("Token parse failure: " + e.getLocalizedMessage)
          case t: TokenVerificationException =>
            // This is an exception of our self. Just throw it further
            throw t
          case e: Throwable =>
            logger.error("Unexpected or unforeseen exception during token verification; throwing it further", e)
            throw new TokenVerificationException("Token verification failure of type " + e.getClass.getSimpleName, e)
        }
    }
  }
}
