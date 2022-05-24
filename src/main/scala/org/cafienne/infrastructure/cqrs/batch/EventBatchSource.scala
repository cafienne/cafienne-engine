package org.cafienne.infrastructure.cqrs.batch

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, TaggedEventSource}

/**
  * Reads model events with a certain tag into batches per persistence id.
  * The batches are considered "full" if a CommitEvent is encountered
  */
trait EventBatchSource extends TaggedEventSource with LazyLogging {

  /**
    * This method must be implemented by the consumer to handle the new batch of ModelEvents
    * When a CommitEvent is encountered, the batch is considered complete.
    * The source handler will then invoke the [[EventBatch#consume()]] method
    * and a next batch will be created when new events come in.
    *
    * @param persistenceId The id of the ModelActor that produced the batch of events
    */
  def createBatch(persistenceId: String): EventBatch

  def batches: Source[EventBatch, NotUsed] = {
    taggedEvents
      .via(new BatchingFlow) // Join all ModelEvents that resulted from a single ModelCommand into a batch
      .filter(_.isDefined).map(_.get) // Is it possible to do this inside the batching flow?
  }

  class BatchingFlow extends GraphStage[FlowShape[ModelEventEnvelope, Option[EventBatch]]] {
    val in: Inlet[ModelEventEnvelope] = Inlet[ModelEventEnvelope]("ModelEvent.BatchingFlow.in")
    val out: Outlet[Option[EventBatch]] = Outlet[Option[EventBatch]]("ModelEvent.BatchingFlow.out")
    val shape: FlowShape[ModelEventEnvelope, Option[EventBatch]] = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      val f: ModelEventEnvelope => Option[EventBatch] = updateBatch
      setHandler(in, () => push(out, f(grab(in))))
      setHandler(out, () => pull(in))
    }

    import scala.collection.mutable

    // Map with batches per persistenceId that are currently streaming events to us.
    private val currentBatches = new mutable.HashMap[String, EventBatch]()

    private def updateBatch(envelope: ModelEventEnvelope): Option[EventBatch] = {
      val persistenceId = envelope.persistenceId
      // Retrieve existing batch, or create a new one
      val batch = currentBatches.getOrElseUpdate(persistenceId, createBatch(persistenceId))
      // Add the event, and if it is a CommitEvent, then the batch is complete and can be sent further
      batch.addEvent(envelope)
      if (envelope.event.isInstanceOf[CommitEvent]) {
        logger.whenDebugEnabled(logger.debug(s"Received batch with ${batch.events.size} events of types ${batch.events.map(_.event.getClass.getSimpleName).toSet.mkString(", ")}"))
        currentBatches.remove(persistenceId)
        Some(batch)
      } else {
        None
      }
    }
  }
}
