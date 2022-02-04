package org.cafienne.service.akkahttp.writer

import akka.actor.{ActorContext, ActorRef}
import akka.persistence.PersistentActor

class CreateEventsInStoreActor extends PersistentActor {
  override def persistenceId: String = "test-events-actor"

  override def receiveRecover: Receive = {
    case other => context.system.log.debug("received unknown event to recover:" + other)
  }

  override def receiveCommand: Receive = {
    case evt => storeAndReply(sender(), evt)
  }

  private def storeAndReply(replyTo: ActorRef, evt: Any)(implicit context: ActorContext): Unit = {
    persist(evt) {
      e =>
        context.system.log.debug(s"persisted $e")
        replyTo ! e
    }
  }

}