package org.cafienne.service.api.projection.task

import akka.Done
import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.cmmn.akka.event.CaseModified
import org.cafienne.humantask.akka.event.HumanTaskEvent
import org.cafienne.infrastructure.cqrs.{OffsetStorage, OffsetStorageProvider, TaggedEventConsumer}
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.tasks.TaskReader

import scala.concurrent.Future

class TaskProjectionsWriter(updater: RecordsPersistence, offsetStorageProvider: OffsetStorageProvider)(implicit override val system: ActorSystem) extends LazyLogging with TaggedEventConsumer {

  import scala.concurrent.ExecutionContext.Implicits.global

  override val offsetStorage: OffsetStorage = offsetStorageProvider.storage("TaskProjectionsWriter")
  override val tag: String = HumanTaskEvent.TAG

  private val transactionCache = new scala.collection.mutable.HashMap[String, TaskTransaction]
  private def getTransaction(taskId: String) = transactionCache.getOrElseUpdate(taskId, new TaskTransaction(taskId, updater))

  def consumeModelEvent(newOffset: Offset, persistenceId: String, sequenceNr: Long, modelEvent: ModelEvent[_]) : Future[Done] = {
    modelEvent match {
      case evt: HumanTaskEvent => {
        val transaction = getTransaction(evt.getActorId)
        transaction.handleEvent(evt).flatMap(_ =>  Future.successful(Done))
      }
      case evt: CaseModified => {
        val transaction = getTransaction(evt.getActorId)
        transactionCache.remove(evt.getActorId)
        transaction.updateLastModifiedInformationInTasks(evt)
        transaction.commit(offsetStorage.name, newOffset).flatMap(_ => {
          TaskReader.inform(evt)
          Future.successful(Done)
        })
      }
      case other => {
        logger.error("Ignoring unexpected model event of type '" + other.getClass.getName() + ". Event has offset: " + newOffset + ", persistenceId: " + persistenceId + ", sequenceNumber: " + sequenceNr)
        Future.successful(Done)
      }
    }
  }
}
