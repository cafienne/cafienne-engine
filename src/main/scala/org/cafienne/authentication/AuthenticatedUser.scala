package org.cafienne.authentication

import com.nimbusds.jwt.JWTClaimsSet
import org.cafienne.actormodel.identity.UserIdentity

class AuthenticatedUser(val token: String, claims: JWTClaimsSet) extends UserIdentity {
  lazy val id: String = claims.getSubject
}
