package org.cafienne.authentication

class AuthenticationException(msg: String) extends RuntimeException(msg) {
  def this(msg: String, cause: Throwable) = {
    this(msg)
    initCause(cause)
  }
}
