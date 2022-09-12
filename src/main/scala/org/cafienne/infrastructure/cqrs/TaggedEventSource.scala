package org.cafienne.infrastructure.cqrs

import akka.NotUsed
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.scaladsl.{RestartSource, Source}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.system.health.HealthMonitor

import scala.concurrent.Future

/**
  * Provides a Source of ModelEvents having a certain Tag (wrapped in an envelope)
  * Reads those events from the given offset onwards.
  */
trait TaggedEventSource extends ReadJournalProvider with ModelEventFilter with LazyLogging  {

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Provide the offset from which we should start sourcing events
    */
  def getOffset: Future[Offset]

  /**
    * Tag to scan events for
    */
  val tag: String

  /**
    * Query to retrieve the events as Source.
    * Defaults to journal.eventsByTag, overrides can alternatively use journal.currentEventsByTag
    * @param offset
    * @return
    */
  def query(offset: Offset): Source[EventEnvelope, NotUsed] = journal().eventsByTag(tag, offset)

  /**
    * Returns the Source
    */
  def taggedEvents: Source[ModelEventEnvelope, NotUsed] =
    restartableTaggedEventSourceFromLastKnownOffset
      .map(reportHealth) // The fact that we receive an event here is an indication that the readJournal is healthy
      .filter(validateModelEvents) // Only interested in ModelEvents, but we log errors if it is an unexpected event or deserialization issue
      .map(ModelEventEnvelope) // Construct a simple wrapper that understands we're dealing with ModelEvents

  def restartableTaggedEventSourceFromLastKnownOffset: Source[EventEnvelope, NotUsed] = {
    RestartSource.withBackoff(Cafienne.config.queryDB.restartSettings) { () =>
      Source.futureSource({
        // First read the last known offset, then get return the events by tag from that offset onwards.
        //  Note: when the source restarts, it will freshly fetch the last known offset, thereby avoiding
        //  consuming that were consumed already successfully before the source had to be restarted.
        getOffset.map { offset: Offset =>
          logger.warn(s"Starting to read '$tag' events from offset " + offset)
          query(offset)
        }
      })
    }
  }

  /**
    * Identity function that has side-effect to indicate a healthy read journal
    */
  def reportHealth(envelope: EventEnvelope): EventEnvelope = {
    // The fact that we receive an event here is an indication that the readJournal is healthy
    HealthMonitor.readJournal.isOK()
    envelope
  }
}
