package org.cafienne.querydb.materializer

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.batch.EventBatch

import scala.concurrent.Future

trait EventBatchTransaction extends LazyLogging {
  def handleEvent(envelope: ModelEventEnvelope): Future[Done]

  def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done]

  def consumeBatch(batch: EventBatch): Future[Done] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    // Create a chain of futures to be executed on the events
    //  Note, this can be improved if specific transactions handle the events in a smarter manner (e.g. all plan item events with the same id in a single shot instead of consecutively)
    var orderedEventHandling: Future[Done] = Future { Done }
    for (event <- batch.events) {
      orderedEventHandling = orderedEventHandling.flatMap { _ => handleEvent(event) }
    }

    orderedEventHandling.flatMap(_ => commit(batch.events.last, batch.commitEvent))
  }
}
