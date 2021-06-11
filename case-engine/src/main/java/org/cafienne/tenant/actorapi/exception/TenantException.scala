package org.cafienne.tenant.actorapi.exception

import org.cafienne.actormodel.command.exception.InvalidCommandException

case class TenantException(msg: String) extends InvalidCommandException(msg)
