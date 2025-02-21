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
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.JWTClaimsSetAwareJWSKeySelector
import org.cafienne.service.infrastructure.configuration.OIDCConfiguration

import java.security.Key

/**
 * This class can select the right keys for the issuer that is indicated in the token
 */
class MultiIssuerJWSKeySelector(config: OIDCConfiguration) extends JWTClaimsSetAwareJWSKeySelector[SecurityContext] {
  // The code below is built based upon this sample: https://jomatt.io/how-to-build-a-multi-tenant-saas-solution-with-spring
  override def selectKeys(header: JWSHeader, claimsSet: JWTClaimsSet, context: SecurityContext): java.util.List[_ <: Key] = {
    // First read the issuer from the token
    val issuerInToken = claimsSet.getIssuer
    if (issuerInToken == null) {
      throw MissingIssuerException
    }

    // Check whether the issuer from the token has been configured
    val configuredIssuer = config.issuers.find(_.issuer == issuerInToken).getOrElse(throw new InvalidIssuerException(s"JWT token has invalid issuer '$issuerInToken', please use another identity provider"))

    // Return the JWS keys
    configuredIssuer.keySelector.selectJWSKeys(header, context)
  }
}
