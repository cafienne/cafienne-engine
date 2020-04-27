package org.cafienne.service.api.projection.cases

import akka.Done
import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.cmmn.akka.event.{CaseEvent, CaseModified}
import org.cafienne.infrastructure.cqrs.{OffsetStorage, OffsetStorageProvider, TaggedEventConsumer}
import org.cafienne.service.api.cases.CaseReader
import org.cafienne.service.api.projection.RecordsPersistence

import scala.concurrent.Future

class CaseProjectionsWriter(persistence: RecordsPersistence, offsetStorageProvider: OffsetStorageProvider)(implicit override val system: ActorSystem) extends LazyLogging with TaggedEventConsumer {

  import scala.concurrent.ExecutionContext.Implicits.global

  override val offsetStorage: OffsetStorage = offsetStorageProvider.storage("CaseProjectionsWriter")
  override val tag: String = CaseEvent.TAG

  private val transactionCache = new scala.collection.mutable.HashMap[String, CaseTransaction]
  private def getTransaction(caseInstanceId: String, tenant: String) = transactionCache.getOrElseUpdate(caseInstanceId, new CaseTransaction(caseInstanceId, tenant, persistence))

  def consumeModelEvent(newOffset: Offset, persistenceId: String, sequenceNr: Long, modelEvent: ModelEvent[_]): Future[Done] = {
    modelEvent match {
      case evt: CaseEvent => {
        val transaction = getTransaction(evt.getActorId, evt.tenant)
        transaction.handleEvent(evt).flatMap(_ => {
          evt match {
            case cm: CaseModified => {
              transactionCache.remove(evt.getActorId)
              transaction.commit(offsetStorage.name, newOffset, cm).flatMap(_ => {
                CaseReader.inform(cm)
                Future.successful(Done)
              })
            }
            case _ => Future.successful(Done)
          }
        })
      }
      case other => {
        logger.error("Ignoring unexpected model event of type '" + other.getClass.getName() + ". Event has offset: " + newOffset + ", persistenceId: " + persistenceId + ", sequenceNumber: " + sequenceNr)
        Future.successful(Done)
      }
    }
  }

}
