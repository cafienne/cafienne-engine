package org.cafienne.querydb.materializer.slick

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope

import scala.concurrent.Future

trait SlickTransaction extends LazyLogging {
  def handleEvent(envelope: ModelEventEnvelope): Future[Done]

  def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done]
}
