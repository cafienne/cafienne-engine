package org.cafienne.infrastructure.cqrs

import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.{Done, NotUsed}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.serialization.{DeserializationFailure, UnrecognizedManifest}
import org.cafienne.system.health.HealthMonitor

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Reads all events with a certain tag, based on last known offset.
  */
trait TaggedEventConsumer extends LazyLogging with ReadJournalProvider {

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Provide the offset from which we should start consuming events
    */
  def getOffset(): Future[Offset]

  /**
    * Tag to scan events for
    */
  val tag: String

  /**
    * This method must be implemented by the consumer to handle the wrapped ModelEvent
    *
    * @param envelope Wrapper around Akka EventEnvelop with a typed event (typed as ModelEvent)
    * @return
    */
  def consumeModelEvent(envelope: ModelEventEnvelope): Future[Done]

  /**
    * Start reading and processing events
    */
  def start(): Unit = {
    runStream() onComplete {
      case Success(_) => //
      case Failure(ex) => {
        logger.error(getClass.getSimpleName + " bumped into an issue that it cannot recover from. Stopping case engine.", ex)
        HealthMonitor.readJournal.hasFailed(ex)
      }
    }
  }

  def runStream(): Future[Done] = {
    restartableTaggedEventSourceFromLastKnownOffset.mapAsync(1)(consumeModelEvent).runWith(Sink.ignore)
  }

  private def restartableTaggedEventSourceFromLastKnownOffset: Source[ModelEventEnvelope, NotUsed] = {
    RestartSource.withBackoff(Cafienne.config.queryDB.restartSettings) { () =>
      Source.futureSource({
        // First read the last known offset, then get return the events by tag from that offset onwards.
        //  Note: when the source restarts, it will freshly fetch the last known offset, thereby avoiding
        //  consuming that were consumed already successfully before the source had to be restarted.
        getOffset().map { offset: Offset =>
          logger.warn(s"Starting to read '$tag' events from offset " + offset)
          journal().eventsByTag(tag, offset).filter(modelEventFilter).map(ModelEventEnvelope)
        }
      })
    }
  }

  /**
    * Filter out only ModelEvents. Also log error messages when other events are encountered (as this is unexpected)
    *
    * @param element
    * @return
    */
  private def modelEventFilter(element: EventEnvelope): Boolean = {
    // The fact that we receive an event here is an indication that the readJournal is healthy
    HealthMonitor.readJournal.isOK()
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