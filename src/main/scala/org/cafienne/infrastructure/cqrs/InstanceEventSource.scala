package org.cafienne.infrastructure.cqrs

import akka.NotUsed
import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging

/**
  * Provides a Source of ModelEvents with the events of a specific persistence id (typically a case or a tenant or a consent group)
  */
trait InstanceEventSource extends ReadJournalProvider with ModelEventFilter with LazyLogging  {
  /**
    * Query to retrieve the events as Source.
    * Defaults to journal.currentEventsByPersistenceId, overrides can alternatively use journal.eventsByTag
    * for a livestream.
    * @param offset
    * @return
    */
  def query(actorId: String): Source[EventEnvelope, NotUsed] = journal().currentEventsByPersistenceId(actorId, 0L, Long.MaxValue)

  /**
    * Composes the Source
    */
  def events(actorId: String): Source[ModelEventEnvelope, NotUsed] =
    query(actorId)
      .filter(validateModelEvents) // Only interested in ModelEvents, but we log errors if it is an unexpected event or deserialization issue
      .map(ModelEventEnvelope) // Construct a simple wrapper that understands we're dealing with ModelEvents
}
