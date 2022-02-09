package org.cafienne.consentgroup.actorapi.exception

import org.cafienne.actormodel.exception.InvalidCommandException

case class ConsentGroupException(msg: String) extends InvalidCommandException(msg)
