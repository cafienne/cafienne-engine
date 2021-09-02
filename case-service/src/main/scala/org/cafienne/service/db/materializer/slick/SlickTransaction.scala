package org.cafienne.service.db.materializer.slick

import akka.Done
import akka.persistence.query.Offset
import org.cafienne.actormodel.event.{ModelEvent, TransactionEvent}

import scala.concurrent.Future

trait SlickTransaction {
  def handleEvent(event: ModelEvent[_], offsetName: String, offset: Offset): Future[Done]

  def commit(offsetName: String, offset: Offset, transactionEvent: TransactionEvent[_]): Future[Done]
}
