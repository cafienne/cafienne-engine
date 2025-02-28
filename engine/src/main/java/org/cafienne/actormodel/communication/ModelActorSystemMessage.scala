package org.cafienne.actormodel.communication

import org.cafienne.actormodel.message.IncomingActorMessage

trait ModelActorSystemMessage extends IncomingActorMessage {

  override def toString: String = getDescription
}
