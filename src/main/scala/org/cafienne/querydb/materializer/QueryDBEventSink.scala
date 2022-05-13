package org.cafienne.querydb.materializer

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.cqrs.batch.{EventBatch, TaggedEventBatchSink}

import scala.concurrent.Future

trait QueryDBEventSink extends TaggedEventBatchSink with LazyLogging {

  def createTransaction(batch: EventBatch): EventBatchTransaction

  override def consumeBatch(batch: EventBatch): Future[Done] = {
    createTransaction(batch).consumeBatch(batch)
  }
}
