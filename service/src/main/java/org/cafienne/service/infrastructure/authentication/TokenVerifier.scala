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
import com.nimbusds.jose.proc.{BadJOSEException, SecurityContext}
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.{BadJWTException, ConfigurableJWTProcessor, DefaultJWTClaimsVerifier, DefaultJWTProcessor}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.service.infrastructure.configuration.OIDCConfiguration
import org.cafienne.system.health.HealthMonitor

import java.text.ParseException
import scala.concurrent.{ExecutionContext, Future}

trait TokenVerifier[T] {
  def verifyToken(token: String): Future[T]
}

class JwtTokenVerifier(val config: OIDCConfiguration)(implicit ec: ExecutionContext) extends TokenVerifier[AuthenticatedUser] with LazyLogging {
  import java.util

  val jwtProcessor: ConfigurableJWTProcessor[SecurityContext] = new DefaultJWTProcessor()

  // Set the required "typ" header "at+jwt" for access tokens issued by the
  // Connect2id server, may not be set by other servers
  //jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier[_](new JOSEObjectType("at+jwt")))

  jwtProcessor.setJWTClaimsSetAwareJWSKeySelector(new MultiIssuerJWSKeySelector(config))

  // Check for the required claims inside the token.
  // "sub", "iat", "exp", "scp", "cid", "jti"
  jwtProcessor.setJWTClaimsSetVerifier(
    new DefaultJWTClaimsVerifier(
      new JWTClaimsSet.Builder().build,
      new util.HashSet[String](util.Arrays.asList("sub", "exp"))
      //NOTE that groups as part of the scope is not required at this moment (like 'roles')
    )
  )

  override def verifyToken(token: String): Future[AuthenticatedUser] = Future {
    var claimsSet: Option[JWTClaimsSet] = None
    if (token.isEmpty) {
      throw MissingTokenException
    }
    try {
      //noinspection ScalaStyle
      val ctx: SecurityContext = null //NOTE this is the way the lib expects to get a None when the context is not required.
      claimsSet = Some(jwtProcessor.process(token, ctx))
      claimsSet.fold(throw new TokenVerificationException("Unable to create claimSet for " + token))(
        cS => {
          HealthMonitor.idp.isOK()
          new AuthenticatedUser(token, cS)
        }
      )
    } catch {
      case rp: RemoteKeySourceException =>
        // TODO: this should return a HTTP code 503 Service Unavailable!
        logger.error("Failure in contacting IDP. Check IDP configuration settings of the case engine.", rp)
        val failure = new CannotReachIDPException("Cannot reach the IDP to validate credentials", rp)
        HealthMonitor.idp.hasFailed(failure)
        throw  failure
      case other: Throwable =>
        HealthMonitor.idp.isOK()
        other match {
          case nje: BadJWTException =>
            //        nje.printStackTrace()
            val exceptionMessage = nje.getMessage
            val missingClaimsMsg = """JWT missing required claims"""
            if (nje.getCause.isInstanceOf[ParseException]) {
              //          println("Failure in parsing token")
              throw new TokenVerificationException("Token parse failure: " + nje.getCause.getLocalizedMessage)
            }

            if (exceptionMessage.contains(missingClaimsMsg)) {
              throw new MissingClaimsException(exceptionMessage.replace(missingClaimsMsg, "JWT token misses claims"))
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
