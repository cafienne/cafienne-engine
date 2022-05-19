package org.cafienne.querydb.materializer

import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.cqrs.batch.EventBatchSource
import org.cafienne.system.health.HealthMonitor

import scala.util.{Failure, Success}

trait QueryDBEventSink extends EventBatchSource with LazyLogging {
  override def createBatch(persistenceId: String): QueryDBEventBatch

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Start reading and processing events
    */
  def start(): Unit = {
    batches
      .mapAsync(1)(_.consume()) // Now handle the batch (would be better if that is done through a real Sink, not yet sure how to achieve that - make EventBatch extend Sink???)
      .runWith(Sink.ignore)
      .onComplete {
        case Success(_) => //
        case Failure(ex) => reportUnhealthy(ex)
      }
  }

  def reportUnhealthy(throwable: Throwable): Unit = {
    // No need to print the stack trace itself here, as that is done in HealthMonitor as well.
    logger.error(s"${getClass.getSimpleName} bumped into an issue that it cannot recover from: ${throwable.getMessage}")
    HealthMonitor.readJournal.hasFailed(throwable)
  }
}
