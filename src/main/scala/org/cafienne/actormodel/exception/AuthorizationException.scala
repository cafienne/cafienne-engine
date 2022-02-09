package org.cafienne.actormodel.exception

case class AuthorizationException(message: String) extends RuntimeException(message)
