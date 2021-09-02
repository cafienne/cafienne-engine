package org.cafienne.service.db.materializer.slick

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.TransactionEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope

import scala.concurrent.Future

trait SlickTransaction extends LazyLogging {
  def handleEvent(envelope: ModelEventEnvelope): Future[Done]

  def commit(envelope: ModelEventEnvelope, transactionEvent: TransactionEvent[_]): Future[Done]
}
