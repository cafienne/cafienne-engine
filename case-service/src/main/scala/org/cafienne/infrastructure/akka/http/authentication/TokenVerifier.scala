package org.cafienne.infrastructure.akka.http.authentication

import java.text.ParseException

import com.nimbusds.jose.{JWSAlgorithm, RemoteKeySourceException}
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.{BadJOSEException, BadJWEException, BadJWSException, JWSKeySelector, JWSVerificationKeySelector, SecurityContext}
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.{BadJWTException, ConfigurableJWTProcessor, DefaultJWTClaimsVerifier, DefaultJWTProcessor}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.CaseSystem

import scala.concurrent.{ExecutionContext, Future}

trait TokenVerifier[T] {
  def verifyToken(token: String): Future[T]
}

class JwtTokenVerifier(keySource: JWKSource[SecurityContext], issuer: String)(implicit ec: ExecutionContext)
    extends TokenVerifier[ServiceUserContext] with LazyLogging {
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
    import scala.jdk.CollectionConverters._
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
          CaseSystem.health.idp.isOK()
          ServiceUserContext(TokenSubject(cS.getSubject), Option(cS.getStringListClaim("groups")).fold(List.empty[String])(groups => groups.asScala.toList))
        }
      )
    } catch {
      case rp: RemoteKeySourceException => {
        // TODO: this should return a HTTP code 503 Service Unavailable!
        logger.error("Failure in contacting IDP. Check IDP configuration settings of the case engine.", rp)
        val failure = new CannotReachIDPException("Cannot reach the IDP to validate credentials", rp)
        CaseSystem.health.idp.hasFailed(failure)
        throw  failure
      }
      case other: Throwable => {
        CaseSystem.health.idp.isOK()
        other match {
          case nje: BadJWTException =>
            //        nje.printStackTrace()
            val exceptionMessage = nje.getMessage
            val missingClaimsMsg = """JWT missing required claims"""
            val invalidIssuerMsg = """JWT "iss" claim doesn't match expected value: """
            val jwtAudienceRejected = """JWT audience rejected"""
            val badJson = """Payload of JWS object is not a valid JSON object"""
            if (nje.getCause.isInstanceOf[ParseException]) {
              //          println("Failure in parsing token")
              throw new TokenVerificationException("Token parse failure: " + nje.getCause.getLocalizedMessage)
            }

            if (exceptionMessage.contains(missingClaimsMsg)) {
              throw new MissingClaimsException(exceptionMessage.replace(missingClaimsMsg, "JWT token misses claims"))
            }
            if (exceptionMessage.contains(invalidIssuerMsg)) {
              val invalidIssuer = exceptionMessage.replace(invalidIssuerMsg, "")
              throw new InvalidIssuerException("JWT token has invalid issuer '" + invalidIssuer + "'. Issuers supported: " + CaseSystem.config.OIDC.issuer)
            }
            throw TokenVerificationException("Invalid token: " + nje.getLocalizedMessage)
          case e: BadJOSEException =>
            // This captures both JWS and JWE exceptions. These are really technical, and logger.debug must be enabled to understand them
            logger.debug("Encountered JWT issues", e)
            throw TokenVerificationException("Token cannot be verified: " + e.getLocalizedMessage)
          case e: ParseException => {
            throw TokenVerificationException("Token parse failure: " + e.getLocalizedMessage)
          }
          case e: Exception => {
            logger.error("Unexpected or unforeseen exception during token verification; throwing it further", e)
            throw new TokenVerificationException("Token verification failure of type " + e.getClass.getSimpleName, e)
          }
        }
      }
    }
  }
}
