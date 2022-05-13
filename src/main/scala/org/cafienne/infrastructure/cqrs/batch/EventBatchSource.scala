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
      .via(new Magic.BatchingFlow) // Join all ModelEvents that resulted from a single ModelCommand into a batch
      .filter(_.isDefined).map(_.get) // Is it possible to do this inside the batching flow?
  }

  object Magic {
    class BatchingFlow(converter: => ModelEventEnvelope => Option[EventBatch] = (new EventBatcher())) extends GraphStage[FlowShape[ModelEventEnvelope, Option[EventBatch]]] {
      val in: Inlet[ModelEventEnvelope] = Inlet[ModelEventEnvelope]("ModelEvent.BatchingFlow.in")
      val out: Outlet[Option[EventBatch]] = Outlet[Option[EventBatch]]("ModelEvent.BatchingFlow.out")
      val shape: FlowShape[ModelEventEnvelope, Option[EventBatch]] = FlowShape.of(in, out)

      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
        val f: ModelEventEnvelope => Option[EventBatch] = converter
        setHandler(in, () => push(out, f(grab(in))))
        setHandler(out, () => pull(in))
      }
    }

    class EventBatcher extends (ModelEventEnvelope => Option[EventBatch]) with LazyLogging {
      private var currentBatch: Option[EventBatch] = None
      override def apply(envelope: ModelEventEnvelope): Option[EventBatch] = {
        if (currentBatch.isEmpty || envelope.persistenceId != currentBatch.get.persistenceId) {
          // We're about to get some new events
          currentBatch = Some(createBatch(envelope.persistenceId))
        }
        currentBatch.get.addEvent(envelope)
        if (envelope.event.isInstanceOf[CommitEvent]) {
          val response = currentBatch
          logger.whenDebugEnabled({
            val batch = currentBatch.get
            logger.debug(s"Received batch with ${batch.events.size} events of types ${batch.events.map(_.event.getClass.getSimpleName).toSet.mkString(", ")}")
          })
          currentBatch = None
          response
        } else {
          None
        }
      }
    }
  }
}
