package org.cafienne.infrastructure.akka.http.authentication

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.{BadJWEException, BadJWSException, JWSKeySelector, JWSVerificationKeySelector, SecurityContext}
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.{BadJWTException, ConfigurableJWTProcessor, DefaultJWTClaimsVerifier, DefaultJWTProcessor}

import scala.concurrent.{ExecutionContext, Future}

trait TokenVerifier[T] {
  def verifyToken(token: String): Future[T]
}
class TokenVerificationException(val msg: String) extends Exception(msg) {
  def this(msg: String, cause: Throwable) {
    this(msg)
    initCause(cause)
  }
}

object TokenVerificationException {
  def apply(msg: String): TokenVerificationException =
    new TokenVerificationException(msg)
  def apply(msg: String, cause: Throwable): TokenVerificationException =
    new TokenVerificationException(msg, cause)

  def unapply(e: TokenVerificationException): Option[(String, Option[Throwable])] =
    Some((e.getMessage, Option(e.getCause)))
}

class JwtTokenVerifier(keySource: JWKSource[SecurityContext], issuer: String)(implicit ec: ExecutionContext)
    extends TokenVerifier[ServiceUserContext] {
  import java.util

  val jwtProcessor: ConfigurableJWTProcessor[SecurityContext] = new DefaultJWTProcessor()

  // Set the required "typ" header "at+jwt" for access tokens issued by the
  // Connect2id server, may not be set by other servers
  //jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier[_](new JOSEObjectType("at+jwt")))

  // The expected JWS algorithm of the access tokens (agreed out-of-band)
  val expectedJWSAlg: JWSAlgorithm = JWSAlgorithm.RS256

  // Configure the JWT processor with a key selector to feed matching public
  // RSA keys sourced from the JWK set URL
  val keySelector: JWSKeySelector[SecurityContext] = new JWSVerificationKeySelector(expectedJWSAlg, keySource)

  jwtProcessor.setJWSKeySelector(keySelector)

  // Check for the required claims inside the token.
  // "sub", "iat", "exp", "scp", "cid", "jti"
  jwtProcessor.setJWTClaimsSetVerifier(
    new DefaultJWTClaimsVerifier(
      new JWTClaimsSet.Builder()
        .issuer(issuer)
        .build,
      new util.HashSet[String](util.Arrays.asList("sub", "exp"))
      //NOTE that groups as part of the scope is not required at this moment (like 'roles')
    )
  )

  override def verifyToken(token: String): Future[ServiceUserContext] = Future {
    import scala.collection.JavaConverters._
    var claimsSet: Option[JWTClaimsSet] = None
    try {
      //noinspection ScalaStyle
      val ctx: SecurityContext = null //NOTE this is the way the lib expects to get a None when the context is not required.
      claimsSet = Some(jwtProcessor.process(token, ctx))
      claimsSet.fold(throw new TokenVerificationException("Unable to create claimSet for " + token))(
        cS => ServiceUserContext(TokenSubject(cS.getSubject), Option(cS.getStringListClaim("groups")).fold(List.empty[String])(groups => groups.asScala.toList)
      ))
    } catch {
      case np: NullPointerException => throw TokenVerificationException("Could not create ServiceUserContext based on token " + token, np)
      case nje: BadJWTException =>
        throw TokenVerificationException("JWT issue, Could not create Claims Set: " + nje.getLocalizedMessage + " with token " + token)
      case bje: BadJWEException =>
        throw TokenVerificationException("JWE issue, Could not create Claims Set: " + bje.getLocalizedMessage + " with token " + token)
      case bje: BadJWSException =>
        throw TokenVerificationException("JWS issue, Could not create Claims Set: " + bje.getLocalizedMessage + " with token " + token)
      case ia: IllegalArgumentException if ia.getMessage.startsWith("Invalid UUID") =>
        throw TokenVerificationException("No UUID for user in Claims Set: " + claimsSet)
      case e: Exception => throw TokenVerificationException("Could not create ServiceUserContext based on claims " + claimsSet, e)
    }
  }
}
