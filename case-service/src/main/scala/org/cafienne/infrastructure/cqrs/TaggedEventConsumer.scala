package org.cafienne.infrastructure.cqrs

import java.util.concurrent.TimeUnit

import akka.{Done, NotUsed}
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.akka.actor.serialization.{DeserializationFailure, UnrecognizedManifest}
import org.cafienne.service.Main

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Reads all events with a certain tag, based on last known offset.
  */
trait TaggedEventConsumer extends LazyLogging with ReadJournalProvider {

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Offset Storage to keep track of last handled event's offset
    */
  val offsetStorage: OffsetStorage
  /**
    * Tag to scan events for
    */
  val tag: String

  /**
    * This method must be implemented by the consumer to handle the tagged ModelEvent
    *
    * @param newOffset
    * @param persistenceId
    * @param sequenceNr
    * @param modelEvent
    * @return
    */
  def consumeModelEvent(newOffset: Offset, persistenceId: String, sequenceNr: Long, modelEvent: ModelEvent[_]): Future[Done]

  /**
    * Start reading and processing events
    */
  def start(): Unit = {
    runStream() onComplete {
      case Success(_) => //
      case Failure(ex) => logger.error(getClass.getSimpleName + " bumped into an issue that it cannot recover from. Stopping case engine.", ex)
    }
  }

  def runStream(): Future[Done] = {
    restartableTaggedEventSourceFromLastKnownOffset.mapAsync(1) {
      case EventEnvelope(newOffset, persistenceId, sequenceNr, evt: ModelEvent[_]) => {
        consumeModelEvent(newOffset, persistenceId, sequenceNr, evt)
      }
      case EventEnvelope(newOffset, persistenceId, sequenceNr, evt: DeserializationFailure) => {
        logger.error("Ignoring event of type '" + evt.manifest + "' with invalid contents. It could not be deserialized. Event has offset: " + newOffset + ", persistenceId: " + persistenceId + ", sequenceNumber: " + sequenceNr, evt.exception)
        logger.debug("Event blob: " + new String(evt.blob))
        Future.successful(Done)
      }
      case EventEnvelope(newOffset, persistenceId, sequenceNr, evt: UnrecognizedManifest) => {
        logger.error("Ignoring unrecognized event of type '" + evt.manifest + "'. Event type is probably deprecated. Event has offset: " + newOffset + ", persistenceId: " + persistenceId + ", sequenceNumber: " + sequenceNr)
        logger.debug("Event contents: " + new String(evt.blob))
        Future.successful(Done)
      }
      case EventEnvelope(newOffset, persistenceId, sequenceNr, evt: Any) => {
        logger.error("Ignoring unknown event of type '" + evt.getClass.getName + "'. Event type is perhaps created through some other product. Event has offset: " + newOffset + ", persistenceId: " + persistenceId + ", sequenceNumber: " + sequenceNr)
        Future.successful(Done)
      }
      case other => {
        logger.error("Received something from the Stream that is not even an EventEnvelope?! It has class " + other.getClass.getName)
        Future.successful(Done)
      }
    }.runWith(Sink.ignore)
  }

  private def restartableTaggedEventSourceFromLastKnownOffset: Source[EventEnvelope, NotUsed] = {
    RestartSource.withBackoff(
      minBackoff = FiniteDuration(3, TimeUnit.SECONDS),
      maxBackoff = FiniteDuration(30, TimeUnit.SECONDS),
      randomFactor = 0.2, // adds 20% "noise" to vary the intervals slightly
      maxRestarts = 20 // limits the amount of restarts to 20
    ) { () =>
      Source.futureSource({
        // First read the last known offset, then get return the events by tag from that offset onwards.
        //  Note: when the source restarts, it will freshly fetch the last known offset, thereby avoiding
        //  consuming that were consumed already successfully before the source had to be restarted.
        offsetStorage.getOffset.map {
          case offset: Offset => {
            logger.debug("Starting from offset " + offset)
            journal.eventsByTag(tag, offset)
          }
          case err: Throwable => {
            logger.error("Received an error while asking for offset; start reading from offset 0; error was: ", err)
            journal.eventsByTag(tag, Offset.noOffset)
          }
        }
      })
    }
  }
}