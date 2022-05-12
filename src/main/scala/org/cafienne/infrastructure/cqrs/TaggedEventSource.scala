package org.cafienne.infrastructure.cqrs

import akka.NotUsed
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.scaladsl.{RestartSource, Source}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.serialization.{DeserializationFailure, UnrecognizedManifest}
import org.cafienne.system.health.HealthMonitor

import scala.concurrent.Future

/**
  * Provides a Source of ModelEvents having a certain Tag (wrapped in an envelope)
  * Reads those events from the given offset onwards.
  */
trait TaggedEventSource extends LazyLogging with ReadJournalProvider {

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Provide the offset from which we should start sourcing events
    */
  def getOffset(): Future[Offset]

  /**
    * Tag to scan events for
    */
  val tag: String

  /**
    * Returns the Source
    */
  def taggedEvents: Source[ModelEventEnvelope, NotUsed] =
    restartableTaggedEventSourceFromLastKnownOffset
      .map(reportHealth) // The fact that we receive an event here is an indication that the readJournal is healthy
      .filter(modelEventFilter) // Only interested in ModelEvents, but we log errors if it is an unexpected event or deserialization issue
      .map(ModelEventEnvelope) // Construct a simple wrapper that understands we're dealing with ModelEvents

  def restartableTaggedEventSourceFromLastKnownOffset: Source[EventEnvelope, NotUsed] = {
    RestartSource.withBackoff(Cafienne.config.queryDB.restartSettings) { () =>
      Source.futureSource({
        // First read the last known offset, then get return the events by tag from that offset onwards.
        //  Note: when the source restarts, it will freshly fetch the last known offset, thereby avoiding
        //  consuming that were consumed already successfully before the source had to be restarted.
        getOffset().map { offset: Offset =>
          logger.warn(s"Starting to read '$tag' events from offset " + offset)
          journal().eventsByTag(tag, offset)
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

  /**
    * Filter out only ModelEvents. Also log error messages when other events are encountered (as this is unexpected)
    *
    * @param element
    * @return
    */
  def modelEventFilter(element: EventEnvelope): Boolean = {
    element match {
      case EventEnvelope(_, _, _, _: ModelEvent) => true
      case _ =>
        val offset = element.offset
        val persistenceId = element.persistenceId
        val sequenceNr = element.sequenceNr
        val eventDescriptor = s"Encountered unexpected event with offset=$offset, persistenceId=$persistenceId, sequenceNumber=$sequenceNr"
        element.event match {
          case evt: DeserializationFailure =>
            logger.error("Ignoring event of type '" + evt.manifest + s"' with invalid contents. It could not be deserialized. $eventDescriptor", evt.exception)
            logger.whenDebugEnabled(logger.debug("Event blob: " + new String(evt.blob)))
          case evt: UnrecognizedManifest =>
            logger.error("Ignoring unrecognized event of type '" + evt.manifest + s"'. Event type is probably deprecated. $eventDescriptor")
            logger.whenDebugEnabled(logger.debug("Event contents: " + new String(evt.blob)))
          case other =>
            logger.error("Ignoring unknown event of type '" + other.getClass.getName + s"'. Event type is perhaps created through some other product. $eventDescriptor")
        }
        false
    }
  }
}
