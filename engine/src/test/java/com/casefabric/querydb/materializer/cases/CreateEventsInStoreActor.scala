package com.casefabric.querydb.materializer.cases

import org.apache.pekko.actor.{ActorContext, ActorRef}
import org.apache.pekko.persistence.PersistentActor
import org.apache.pekko.persistence.journal.Tagged
import com.casefabric.actormodel.event.ModelEvent

import scala.jdk.CollectionConverters.CollectionHasAsScala

class CreateEventsInStoreActor extends PersistentActor {
  override def persistenceId: String = self.path.name

  override def receiveRecover: Receive = {
    case other => context.system.log.debug("received unknown event to recover:" + other)
  }

  override def receiveCommand: Receive = {
    case evt => storeAndReply(sender(), evt)
  }

  private def storeAndReply(replyTo: ActorRef, evt: Any)(implicit context: ActorContext): Unit = {
    def addTags(event: Any): Any = {
      event match {
        case event1: ModelEvent => Tagged(event, event1.tags.asScala.toSet)
        case _ => event
      }
    }

    persist(addTags(evt)) {
      e =>
        context.system.log.debug(s"persisted $e")
        replyTo ! evt
    }
  }

}