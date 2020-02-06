package org.cafienne.infrastructure.akka.http.authentication

class TokenVerificationException(val msg: String) extends SecurityException(msg) {
  def this(msg: String, cause: Throwable) {
    this(msg)
    initCause(cause)
  }
}

object TokenVerificationException {
  def apply(msg: String): TokenVerificationException =
    new TokenVerificationException(msg)
  def apply(msg: String, cause: Throwable): TokenVerificationException =
    new TokenVerificationException(msg, cause)

  def unapply(e: TokenVerificationException): Option[(String, Option[Throwable])] =
    Some((e.getMessage, Option(e.getCause)))
}

class MissingTokenException() extends TokenVerificationException("Authorization token is missing")
class InvalidJWTException() extends TokenVerificationException("Authorization token does not match JWT standard")
class MissingClaimsException(msg: String) extends TokenVerificationException(msg)
class InvalidIssuerException(msg: String) extends TokenVerificationException(msg)
class CannotReachIDPException(msg: String, cause: Throwable) extends TokenVerificationException(msg, cause)
