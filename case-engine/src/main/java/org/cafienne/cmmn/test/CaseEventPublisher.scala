package org.cafienne.cmmn.test

import akka.actor.ActorSystem
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.cqrs.ReadJournalProvider

import scala.concurrent.Future

class CaseEventPublisher(listener: CaseEventListener, implicit val system: ActorSystem) extends ReadJournalProvider{
  val source: Source[EventEnvelope, NotUsed] = journal().eventsByTag(ModelEvent.TAG, Offset.noOffset)
  source.mapAsync(1) {
    case EventEnvelope(newOffset, persistenceId, sequenceNr, evt: AnyRef) => {
      listener.handle(evt)
      Future.successful(Done)
    }
    case _ => Future.successful(Done) // Ignore other events
  }.runWith(Sink.ignore)
}
