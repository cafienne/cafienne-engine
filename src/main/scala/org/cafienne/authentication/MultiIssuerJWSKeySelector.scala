package org.cafienne.authentication

import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.JWTClaimsSetAwareJWSKeySelector

import java.security.Key
import java.util

class MultiIssuerJWSKeySelector(issuers: ) extends JWTClaimsSetAwareJWSKeySelector[SecurityContext] {

  //TODO see this sample: https://jomatt.io/how-to-build-a-multi-tenant-saas-solution-with-spring

  //TODO other classes with TODOs TokenVerifier, AuthenticationDirectives, AuthenticatedRoute, CafienneConfig

  override def selectKeys(header: JWSHeader, claimsSet: JWTClaimsSet, context: SecurityContext): util.List[_ <: Key] = {

  }


}
