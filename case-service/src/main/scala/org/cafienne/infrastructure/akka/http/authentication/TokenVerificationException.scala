package org.cafienne.infrastructure.akka.http.authentication

class TokenVerificationException(val msg: String) extends AuthenticationException(msg) {
  def this(msg: String, cause: Throwable) = {
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

object MissingTokenException extends TokenVerificationException("Authorization token is missing")
class MissingClaimsException(msg: String) extends TokenVerificationException(msg)
class InvalidIssuerException(msg: String) extends TokenVerificationException(msg)
class CannotReachIDPException(msg: String, cause: Throwable) extends TokenVerificationException(msg, cause)
