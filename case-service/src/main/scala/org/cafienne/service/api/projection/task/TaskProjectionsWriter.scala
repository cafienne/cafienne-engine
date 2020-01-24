package org.cafienne.service.api.projection.task

import akka.Done
import akka.actor.ActorSystem
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.akka.actor.serialization.{DeserializationFailure, UnrecognizedManifest}
import org.cafienne.cmmn.akka.event.CaseModified
import org.cafienne.humantask.akka.event.HumanTaskEvent
import org.cafienne.infrastructure.cqrs.ReadJournalProvider
import org.cafienne.infrastructure.jdbc.OffsetStorage
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.tasks.TaskReader

import scala.concurrent.Future
import scala.util.{Failure, Success}

class TaskProjectionsWriter(updater: RecordsPersistence, offsetStorage: OffsetStorage)(implicit override val system: ActorSystem) extends LazyLogging with OffsetStorage with ReadJournalProvider {

  import scala.concurrent.ExecutionContext.Implicits.global

  val offsetStorageName = "TaskProjectionsWriter"

  val transactionCache = new scala.collection.mutable.HashMap[String, TaskTransaction]

  /**
    * Returns the currently running transaction for the case with this specific id.
    * If none is running, a new one is created.
    *
    * @param taskId
    * @return
    */
  private def getTransaction(taskId: String): TaskTransaction = {
    transactionCache.getOrElseUpdate(taskId, new TaskTransaction(taskId, updater))
  }

  def start(): Unit = {
    offsetStorage.getOffset(offsetStorageName).onComplete {
      case Success(offset: Offset) => {
        logger.debug("Starting from offset " + offset)
        readStream(offset)
      }
      case Failure(err) => {
        logger.error("Received an error while asking for offset; start reading from offset 0; error was: ", err)
        readStream(Offset.noOffset)
      }
    }
  }

  def readStream(offset: Offset = Offset.noOffset): Unit =
    runStream(offset) onComplete {
      case Success(_) => //
      case Failure(ex) => logger.error("Writer stream has an issue ", ex)
    }

  def runStream(originalOffset: Offset): Future[akka.Done] = {
    val source: Source[EventEnvelope, akka.NotUsed] = journal.eventsByTag(HumanTaskEvent.TAG, originalOffset)
    source.mapAsync(1) {
      case EventEnvelope(newOffset, _, _, evt: HumanTaskEvent) => {
        val transaction = getTransaction(evt.getActorId)
        transaction.handleEvent(evt).flatMap(_ =>  Future.successful(Done))
      }
      case EventEnvelope(newOffset, _, _, evt: CaseModified) => {
        val transaction = getTransaction(evt.getActorId)
        transactionCache.remove(evt.getActorId)
        transaction.updateLastModifiedInformationInTasks(evt)
        transaction.commit(offsetStorageName, newOffset).flatMap(_ => {
          TaskReader.inform(evt)
          Future.successful(Done)
        })
      }
      case EventEnvelope(newOffset, persistenceId, sequenceNr, evt: ModelEvent[_]) => {
        logger.error("Ignoring unexpected model event of type '" + evt.getClass.getName() + ". Event has offset: " + newOffset + ", persistenceId: " + persistenceId + ", sequenceNumber: " + sequenceNr)
        Future.successful(Done)
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
}
