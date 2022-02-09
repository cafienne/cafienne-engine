package org.cafienne.authentication

import com.nimbusds.jwt.JWTClaimsSet

class AuthenticatedUser(val token: String, claims: JWTClaimsSet) {
  lazy val userId = claims.getSubject
}
