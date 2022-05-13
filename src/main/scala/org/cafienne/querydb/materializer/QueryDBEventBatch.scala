package org.cafienne.querydb.materializer

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.batch.EventBatch

import scala.concurrent.Future

trait QueryDBEventBatch extends EventBatch {
  def createTransaction: EventBatchTransaction

  override def consume(): Future[Done] = {
    val tx: EventBatchTransaction = createTransaction
    import scala.concurrent.ExecutionContext.Implicits.global

    // Create a chain of futures to be executed on the events
    //  Note, this can be improved if specific transactions handle the events in a smarter manner (e.g. all plan item events with the same id in a single shot instead of consecutively)
    var orderedEventHandling: Future[Done] = Future { Done }
    for (event <- events) {
      orderedEventHandling = orderedEventHandling.flatMap { _ => tx.handleEvent(event) }
    }

    orderedEventHandling.flatMap(_ => tx.commit(events.last, commitEvent))
  }
}

trait EventBatchTransaction extends LazyLogging {
  def handleEvent(envelope: ModelEventEnvelope): Future[Done]

  def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done]
}
