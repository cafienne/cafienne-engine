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

import org.apache.pekko.http.scaladsl.server.directives.Credentials
import org.apache.pekko.http.scaladsl.server.{Directive1, Directives}
import org.cafienne.actormodel.identity.{IdentityProvider, PlatformUser}
import org.cafienne.querydb.lastmodified.LastModifiedHeader

import scala.concurrent.{ExecutionContext, Future}

/**
  * This authentication directive supports the use of Open ID Connect (OIDC)
  * It is based on the @see https://connect2id.com/products/nimbus-jose-jwt Nimbus JOSE library
  *
  *
  */
trait AuthenticationDirectives extends Directives {

  implicit val ex: ExecutionContext

  //TODO make the token verifier initialize with the list of tuple (issuer, keysource) as defined in AuthenticatedRoute
  lazy private val jwtTokenVerifier = new JwtTokenVerifier()

  //IdentityProvider to get the user
  protected val userCache: IdentityProvider

  def authenticatedUser(): Directive1[AuthenticatedUser] = {
    authenticateOAuth2Async("service", verifyJWTToken)
  }

  def platformUser(tlm: LastModifiedHeader): Directive1[PlatformUser] = {
    authenticateOAuth2Async("service", c => {
      jwtToPlatformUser(c, tlm)
    })
  }

  private def verifyJWTToken(credentials: Credentials): Future[Option[AuthenticatedUser]] = {
    credentials match {
      case Credentials.Provided(token) => jwtTokenVerifier.verifyToken(token).map(ctx => Some(ctx))
      case Credentials.Missing => Future.failed(MissingTokenException)
    }
  }

  private def jwtToPlatformUser(credentials: Credentials, tlm: LastModifiedHeader): Future[Option[PlatformUser]] = {
    credentials match {
      case Credentials.Provided(token) => {
        for {
          authenticatedUser <- jwtTokenVerifier.verifyToken(token)
          cachedUser <- userCache.getPlatformUser(authenticatedUser, tlm)
        } yield Some(cachedUser)
      }
      case Credentials.Missing => Future.failed(MissingTokenException)
    }
  }
}
