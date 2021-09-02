package org.cafienne.service.db.materializer.slick

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.TransactionEvent
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, TaggedEventConsumer}
import org.cafienne.service.db.materializer.LastModifiedRegistration

import scala.concurrent.Future

trait SlickEventMaterializer extends TaggedEventConsumer with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  val lastModifiedRegistration: LastModifiedRegistration
  private val transactionCache = new scala.collection.mutable.HashMap[String, SlickTransaction]

  def createTransaction(actorId: String, tenant: String): SlickTransaction

  override def consumeModelEvent(envelope: ModelEventEnvelope): Future[Done] = {
    val actorId = envelope.persistenceId
    val offset = envelope.offset
    val transaction = getTransaction(actorId, envelope.event.tenant)
    transaction.handleEvent(envelope).flatMap(_ => {
      envelope.event match {
        case commitEvent: TransactionEvent[_] => {
          transactionCache.remove(actorId)
          for {
            commitTransaction <- {
              logger.whenDebugEnabled(logger.debug(s"'${getClass.getSimpleName}' handled events up to offset $offset"))
              transaction.commit(envelope, commitEvent)
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
