package org.cafienne.authentication

import com.nimbusds.jose.jwk.source.{JWKSource, RemoteJWKSet}
import com.nimbusds.jose.proc.{JWSKeySelector, JWSVerificationKeySelector, SecurityContext}
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.JWTClaimsSetAwareJWSKeySelector
import com.nimbusds.oauth2.sdk.id._
import com.nimbusds.openid.connect.sdk.op._
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.config.api.OIDCConfig

import java.net.URL
import java.security.Key

class MultiIssuerJWSKeySelector extends JWTClaimsSetAwareJWSKeySelector[SecurityContext] {
  // The code below is built based upon this sample: https://jomatt.io/how-to-build-a-multi-tenant-saas-solution-with-spring
  val issuers: Map[String, JWSKeySelector[SecurityContext]] = MultiIssuerJWSKeySelector.configuredIssuers


  override def selectKeys(header: JWSHeader, claimsSet: JWTClaimsSet, context: SecurityContext): java.util.List[_ <: Key] = {
    val issuer = claimsSet.getIssuer

    // Exception if we cannot find the expected idp
    def unknownIDP = throw new InvalidIssuerException(s"JWT token has invalid issuer '$issuer', please use another identity provider")
    issuers.get(issuer).fold(unknownIDP)(_.selectJWSKeys(header, context))
  }
}

object MultiIssuerJWSKeySelector extends LazyLogging {
  val configuredIssuers: Map[String, JWSKeySelector[SecurityContext]] = readIssuersConfiguration()

  def initialize(): Unit = {
    logger.warn(s"Cafienne HTTP Server is configured with ${configuredIssuers.size} identity providers: ${configuredIssuers.keys.mkString("\n- ", "\n- ", "")}")
  }

  def readIssuersConfiguration(): Map[String, JWSKeySelector[SecurityContext]] = {
    val issuers = Cafienne.config.OIDCList.map(config => {
      if (config.keysUrl.nonEmpty) {
        logger.info(s"Reading static info for IDP ${config.issuer}")
        Some(readStaticConfiguration(config))
      } else if (config.connectUrl.nonEmpty) {
        logger.info(s"Reading dynamic info for IDP ${config.issuer}")
        Some(readDynamicConfiguration(config))
      } else if (config.issuer.nonEmpty) {
        logger.warn(s"Missing OIDC configuration information on IDP '${config.issuer}'")
        None
      } else {
        logger.warn(s"Encountered empty IDP configuration; this configuration will be skipped")
        None
      }
    }).filter(_.isDefined).map(_.get)

    if (issuers.isEmpty) {
      logger.error("ERROR: Missing valid OIDC configuration")
    }

    issuers.toMap[String, JWSKeySelector[SecurityContext]]
  }

  def readStaticConfiguration(config: OIDCConfig): (String, JWSKeySelector[SecurityContext]) = {
    val keySource: JWKSource[SecurityContext] = new RemoteJWKSet(new URL(config.keysUrl))
    val issuer: String = config.issuer
    // The expected JWS algorithm of the access tokens (agreed out-of-band)
    val expectedJWSAlg: JWSAlgorithm = JWSAlgorithm.RS256 // TODO: could this become configurable as well?

    // Configure the JWT processor with a key selector to feed matching public
    // RSA keys sourced from the JWK set URL
    val keySelector: JWSKeySelector[SecurityContext] = new JWSVerificationKeySelector(expectedJWSAlg, keySource)
    (issuer, keySelector)
  }

  def readDynamicConfiguration(config: OIDCConfig): (String, JWSKeySelector[SecurityContext]) = {
//    println(s"\nRetrieving metadata info for IDP Issuer ${config.issuer} from well known url ${config.connectUrl} ")

    val metadata: OIDCProviderMetadata = OIDCProviderMetadata.resolve(new Issuer(config.connectUrl))
//    println("Metaprovider json: " + JSONReader.parse(metadata.toJSONObject.toJSONString()))
    val keySource: JWKSource[SecurityContext] = new RemoteJWKSet(metadata.getJWKSetURI.toURL)
    val issuer: String = metadata.getIssuer.getValue
    val algorithms = new java.util.HashSet(metadata.getIDTokenJWSAlgs)
    // Configure the JWT processor with a key selector to feed matching public
    // RSA keys sourced from the JWK set URL
    val keySelector: JWSKeySelector[SecurityContext] = new JWSVerificationKeySelector(algorithms, keySource)
    (issuer, keySelector)
  }
}
