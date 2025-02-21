package org.cafienne.service.infrastructure.authentication

import com.nimbusds.jwt.proc.DefaultJWTProcessor

class AuthenticatedUserReader(val verifier: TokenVerifier) extends DefaultJWTProcessor[TokenContext] {

  // Set the required "typ" header "at+jwt" for access tokens issued by the
  // Connect2id server, may not be set by other servers
  //jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier[_](new JOSEObjectType("at+jwt")))

  setJWTClaimsSetAwareJWSKeySelector(new MultiIssuerJWSKeySelector(verifier.config))

  // Check for the required claims inside the token.
  // JWT says "sub", "iat", "exp", "scp", "cid", "jti"
  // We only verify "exp", and by default "sub" (or an 'issuer' based alternative if configured, like "oid" for MS Entra)
  setJWTClaimsSetVerifier(new ClaimsVerifier(verifier.config))

  def createUserContext(token: String): TokenContext = new TokenContext(token)
}
