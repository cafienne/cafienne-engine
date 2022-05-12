package org.cafienne.querydb.materializer.slick

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.batch.{EventBatch, TaggedEventBatchSink}

import scala.concurrent.Future

trait QueryDBEventSink extends TaggedEventBatchSink with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  def createTransaction(envelope: ModelEventEnvelope): SlickTransaction

  override def consumeBatch(batch: EventBatch): Future[Done] = {
    val transaction = createTransaction(batch.events.head)

    // Create a chain of futures to be executed on the events
    //  Note, this can be improved if specific transactions handle the events in a smarter manner (e.g. all plan item events with the same id in a single shot instead of consecutively)
    var orderedEventHandling: Future[Done] = Future{Done}
    for(item <- batch.events) {
      orderedEventHandling = orderedEventHandling.flatMap { _ => transaction.handleEvent(item) }
    }

    orderedEventHandling.flatMap(_ => transaction.commit(batch.events.last, batch.commitEvent))
  }
}
