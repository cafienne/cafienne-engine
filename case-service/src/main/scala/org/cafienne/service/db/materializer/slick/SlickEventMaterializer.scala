package org.cafienne.service.db.materializer.slick

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.{ModelEvent, TransactionEvent}
import org.cafienne.infrastructure.cqrs.TaggedEventConsumer
import org.cafienne.service.db.materializer.LastModifiedRegistration

import scala.concurrent.Future

trait SlickEventMaterializer extends TaggedEventConsumer with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  val lastModifiedRegistration: LastModifiedRegistration
  private val transactionCache = new scala.collection.mutable.HashMap[String, SlickTransaction]

  def createTransaction(actorId: String, tenant: String): SlickTransaction

  def consumeModelEvent(newOffset: Offset, persistenceId: String, sequenceNr: Long, modelEvent: ModelEvent[_]): Future[Done] = {
    val transaction = getTransaction(modelEvent.getActorId, modelEvent.tenant)
    transaction.handleEvent(modelEvent, offsetStorage.storageName, newOffset).flatMap(_ => {
      modelEvent match {
        case commitEvent: TransactionEvent[_] => {
          transactionCache.remove(modelEvent.getActorId)
          for {
            commitTransaction <- {
              logger.whenDebugEnabled(logger.debug(s"Updating '${offsetStorage.storageName}' offset to $newOffset"))
              transaction.commit(offsetStorage.storageName, newOffset, commitEvent)
            }
            informPendingQueries <- {
              lastModifiedRegistration.handle(commitEvent)
              Future.successful(Done)
            }
          } yield (commitTransaction, informPendingQueries)._2
        }
        case _ => Future.successful(Done)
      }
    })
  }

  private def getTransaction(actorId: String, tenant: String) = transactionCache.getOrElseUpdate(actorId, createTransaction(actorId, tenant))
}
