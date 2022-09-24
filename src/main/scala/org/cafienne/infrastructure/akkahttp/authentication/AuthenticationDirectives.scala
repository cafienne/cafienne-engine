package org.cafienne.infrastructure.akkahttp.authentication

import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive1, Directives}
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.authentication.{AuthenticatedUser, JwtTokenVerifier, MissingTokenException}

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

  def platformUser(tlm: Option[String]): Directive1[PlatformUser] = {
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

  private def jwtToPlatformUser(credentials: Credentials, tlm: Option[String]): Future[Option[PlatformUser]] = {
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
