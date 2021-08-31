package org.cafienne.infrastructure.akka.http.authentication

import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive1, Directives}
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.identity.IdentityProvider

import scala.concurrent.{ExecutionContext, Future}

/**
  * This authentication directive supports the use of Open ID Connect (OIDC)
  * It is based on the @see https://connect2id.com/products/nimbus-jose-jwt Nimbus JOSE library
  *
  *
  */
trait AuthenticationDirectives {
  self: Directives =>

  //  implicit val ec = ExecutionContext.global
  lazy private val jwtTokenVerifier = new JwtTokenVerifier(keySource: JWKSource[SecurityContext], issuer)
  // The public RSA keys to validate the signatures will be sourced from the
  // OAuth 2.0 server's JWK set, published at a well-known URL. The RemoteJWKSet
  // object caches the retrieved keys to speed up subsequent look-ups and can
  // also handle key-rollover
  protected val keySource: JWKSource[SecurityContext]
  //Issuer that issues the token.
  protected val issuer: String

  protected implicit val ec: ExecutionContext
  //IdentityProvider to get the user
  protected val userCache: IdentityProvider

  def user(tlm: Option[String]): Directive1[PlatformUser] = {
    authenticateOAuth2Async("service", c => {
      jwtToServiceUserAuthenticator(c, tlm)
    })
  }

  private def jwtToServiceUserAuthenticator(credentials: Credentials, tlm: Option[String]): Future[Option[PlatformUser]] = {
    credentials match {
      case Credentials.Provided(token) => {
        for {
          usrCtx <- jwtTokenVerifier.verifyToken(token)
          cachedUser <- userCache.getUser(usrCtx.subject.value, tlm)
        } yield Some(cachedUser)
      }
      case Credentials.Missing => Future.failed(MissingTokenException)
    }
  }

  //  def user(realm: String): Directive1[ServiceUserContext] = {
  //    authenticateOAuth2Async(realm, jwtToServiceUserAuthenticator)
  //  }
}
