package org.cafienne.authentication

import com.nimbusds.jose.jwk.source.{JWKSource, RemoteJWKSet}
import com.nimbusds.jose.proc.{JWSVerificationKeySelector, SecurityContext, JWSKeySelector}
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.JWTClaimsSetAwareJWSKeySelector
import org.cafienne.infrastructure.Cafienne

import java.net.URL
import java.security.Key

class MultiIssuerJWSKeySelector extends JWTClaimsSetAwareJWSKeySelector[SecurityContext] {

  // The code below is built based upon this sample: https://jomatt.io/how-to-build-a-multi-tenant-saas-solution-with-spring

  val configuredIssuers: Map[String, JWSKeySelector[SecurityContext]] = readIssuersConfiguration()


  override def selectKeys(header: JWSHeader, claimsSet: JWTClaimsSet, context: SecurityContext): java.util.List[_ <: Key] = {
    val issuer = claimsSet.getIssuer
    // Exception if we cannot find the expected idp
    def unknownIDP = throw new InvalidIssuerException(s"JWT token has invalid issuer '$issuer', please use another identity provider")
    configuredIssuers.get(issuer).fold(unknownIDP)(_.selectJWSKeys(header, context))
  }

  def readIssuersConfiguration(): Map[String, JWSKeySelector[SecurityContext]] = {
    val issuers = Cafienne.config.OIDCList.map(config => {
      val keySource: JWKSource[SecurityContext] = new RemoteJWKSet(new URL(config.keysUrl))
      val issuer: String = config.issuer
      // The expected JWS algorithm of the access tokens (agreed out-of-band)
      val expectedJWSAlg: JWSAlgorithm = JWSAlgorithm.RS256 // TODO: could this become configurable as well?

      // Configure the JWT processor with a key selector to feed matching public
      // RSA keys sourced from the JWK set URL
      val keySelector: JWSKeySelector[SecurityContext] = new JWSVerificationKeySelector(expectedJWSAlg, keySource)
      (issuer, keySelector)
    })

    issuers.toMap[String, JWSKeySelector[SecurityContext]]
  }
}
