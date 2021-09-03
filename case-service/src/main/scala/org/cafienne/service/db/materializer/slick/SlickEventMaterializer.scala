package org.cafienne.service.db.materializer.slick

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, TaggedEventConsumer}

import scala.concurrent.Future

trait SlickEventMaterializer extends TaggedEventConsumer with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val transactionCache = new scala.collection.mutable.HashMap[String, SlickTransaction]

  def createTransaction(envelope: ModelEventEnvelope): SlickTransaction

  override def consumeModelEvent(envelope: ModelEventEnvelope): Future[Done] = {
    val transaction = transactionCache.getOrElseUpdate(envelope.persistenceId, createTransaction(envelope))
    // If it is a commit event, then we should remove and also commit the transaction after handling it.
    envelope.event match {
      case commitEvent: CommitEvent =>
        transactionCache.remove(envelope.persistenceId)
        transaction.handleEvent(envelope).flatMap(_ => transaction.commit(envelope, commitEvent))
      case _ => transaction.handleEvent(envelope)
    }
  }
}
