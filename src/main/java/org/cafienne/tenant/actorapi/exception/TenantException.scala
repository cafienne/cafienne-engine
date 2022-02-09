package org.cafienne.tenant.actorapi.exception

import org.cafienne.actormodel.exception.InvalidCommandException

case class TenantException(msg: String) extends InvalidCommandException(msg)
