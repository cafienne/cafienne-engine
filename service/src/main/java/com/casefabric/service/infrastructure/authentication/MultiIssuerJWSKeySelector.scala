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

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.jwk.source.{JWKSource, JWKSourceBuilder}
import com.nimbusds.jose.proc.{JWSKeySelector, JWSVerificationKeySelector, SecurityContext}
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.JWTClaimsSetAwareJWSKeySelector
import com.typesafe.scalalogging.LazyLogging

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

  def readIssuersConfiguration(): Map[String, JWSKeySelector[SecurityContext]] = {
    val issuers = OIDCConfiguration.config.issuers.map(metadata => {
      val keySource: JWKSource[SecurityContext] = JWKSourceBuilder.create(metadata.getJWKSetURI.toURL).build()
      val issuer: String = metadata.getIssuer.getValue
      val algorithms = new java.util.HashSet(metadata.getIDTokenJWSAlgs)
      // Configure the JWT processor with a key selector to feed matching public
      // RSA keys sourced from the JWK set URL
      val keySelector: JWSKeySelector[SecurityContext] = new JWSVerificationKeySelector(algorithms, keySource)
      (issuer, keySelector)
    })

    if (issuers.isEmpty) {
      logger.error("ERROR: Missing valid OIDC configuration")
    }

    issuers.toMap[String, JWSKeySelector[SecurityContext]]
  }
}
