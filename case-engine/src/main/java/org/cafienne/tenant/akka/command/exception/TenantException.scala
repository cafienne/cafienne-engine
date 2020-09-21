package org.cafienne.tenant.akka.command.exception

import org.cafienne.akka.actor.command.exception.InvalidCommandException

case class TenantException(msg: String) extends InvalidCommandException(msg)
