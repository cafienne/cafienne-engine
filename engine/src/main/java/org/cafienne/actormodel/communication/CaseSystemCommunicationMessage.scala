package org.cafienne.actormodel.communication

import org.cafienne.actormodel.message.IncomingActorMessage

trait CaseSystemCommunicationMessage extends IncomingActorMessage {

  override def toString: String = getDescription
}
