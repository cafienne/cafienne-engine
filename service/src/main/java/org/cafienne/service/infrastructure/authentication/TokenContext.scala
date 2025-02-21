package org.cafienne.service.infrastructure.authentication

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import org.cafienne.service.infrastructure.configuration.IssuerConfiguration

class TokenContext(val token: String) extends SecurityContext {
  if (token.isEmpty) {
    throw MissingTokenException
  }

  private var _user: Option[AuthenticatedUser] = None
  private var _issuer: Option[IssuerConfiguration] = None

  /**
   * This method is invoked from the KeySelector
   */
  def setIssuer(issuer: IssuerConfiguration): Unit = this._issuer = Some(issuer)

  def issuer: IssuerConfiguration = _issuer.get


  /**
   * This method is invoked from the ClaimsVerifier
   */
  def createUser(id: String, claims: JWTClaimsSet): Unit = this._user = Some(new AuthenticatedUser(id, token, claims))

  /**
   * Returns the authenticatedUser object that is converted from the token
   */
  def authenticatedUser: AuthenticatedUser = this._user.get
}
