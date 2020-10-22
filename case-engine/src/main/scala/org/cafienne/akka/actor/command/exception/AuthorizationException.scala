package org.cafienne.akka.actor.command.exception

case class AuthorizationException(message: String) extends RuntimeException(message)
