package org.cafienne.actormodel.command.exception

case class AuthorizationException(message: String) extends RuntimeException(message)
