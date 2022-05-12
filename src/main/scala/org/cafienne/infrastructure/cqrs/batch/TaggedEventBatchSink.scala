package org.cafienne.infrastructure.cqrs.batch

import akka.Done
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.scaladsl.Sink
import akka.stream.stage.{GraphStage, GraphStageLogic}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, TaggedEventSource}
import org.cafienne.system.health.HealthMonitor

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Reads model events with a certain tag into batches per persistence id.
  * The batches are considered "full" if a CommitEvent is encountered
  */
trait TaggedEventBatchSink extends LazyLogging with TaggedEventSource {

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * This method must be implemented by the consumer to handle the new batch of ModelEvents
    *
    * @param batch Batch with a collection of ModelEvents that happened on a ModelActor upon handling a single ModelCommand.
    * @return
    */
  def consumeBatch(batch: EventBatch): Future[Done]

  /**
    * Start reading and processing events
    */
  def start(): Unit = {
    taggedEvents
      .via(CombineEventsFlow(new EventBatcher)) // Join all ModelEvents that resulted from a single ModelCommand into a batch
      .filter(_.isDefined).map(_.get) // Would be better if we can do without the option, and have that inside the batcher or the combine events flow
      .mapAsync(1)(consumeBatch) // Now handle the batch (would be better if that is done through a real Sink, not yet sure how to achieve that)
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

class EventBatcher[T <: AnyRef] extends (ModelEventEnvelope => Option[EventBatch]) with LazyLogging {
  private var currentBatch: EventBatch = _
  override def apply(envelope: ModelEventEnvelope): Option[EventBatch] = {
    if (currentBatch == null || envelope.persistenceId != currentBatch.persistenceId) {
      // We're about to get some new events
      currentBatch = new EventBatch(envelope.persistenceId)
    }
    currentBatch.addEvent(envelope)
    if (envelope.event.isInstanceOf[CommitEvent]) {
      val response = Some(currentBatch)
      logger.whenDebugEnabled(logger.debug(s"Received batch with ${currentBatch.events.size} events of types ${currentBatch.events.map(_.event.getClass.getSimpleName).toSet.mkString(", ")}"))
      currentBatch = null
      response
    } else {
      None
    }
  }
}

object CombineEventsFlow {
  def apply[I, O](converter: => I => Option[O]): CombineEventsFlow[I, O] = new CombineEventsFlow[I, O](converter)
}

class CombineEventsFlow[I, O](converter: => I => Option[O]) extends GraphStage[FlowShape[I, Option[O]]] {
  val in: Inlet[I] = Inlet[I]("CombineEventsFlow.in")
  val out: Outlet[Option[O]] = Outlet[Option[O]]("CombineEventsFlow.out")
  val shape: FlowShape[I, Option[O]] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    val f: I => Option[O] = converter
    setHandler(in, () => push(out, f(grab(in))))
    setHandler(out, () => pull(in))
  }
}
