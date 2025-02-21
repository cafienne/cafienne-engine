package org.cafienne.service.infrastructure.authentication

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import org.cafienne.service.infrastructure.configuration.OIDCConfiguration

import java.util

class ClaimsVerifier(config: OIDCConfiguration) extends DefaultJWTClaimsVerifier[TokenContext](new JWTClaimsSet.Builder().build, new util.HashSet(util.Arrays.asList("exp"))) {
  override def verify(claims: JWTClaimsSet, context:TokenContext): Unit = {
    // First let the parent class verify 'exp' claim to have a valid date and avoid expired tokens
    super.verify(claims, context)

    val userIdClaim = context.issuer.userIdClaim

    // Reading the claim as string may result in failures like "Token parse failure: The sub claim is not a String"
    val userId = claims.getStringClaim(userIdClaim)
    if (userId == null) {
      throw new MissingClaimsException(s"Invalid token: claim '$userIdClaim' is missing")
    }
    context.createUser(userId, claims)
  }
}
