/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.infrastructure.cqrs.batch

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.stage.{GraphStage, GraphStageLogic}
import org.apache.pekko.stream.{Attributes, FlowShape, Inlet, Outlet}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, TaggedEventSource}

/**
  * Reads model events with a certain tag into batches per persistence id.
  * The batches are considered "full" if a CommitEvent is encountered
  */
trait EventBatchSource[T <: EventBatch] extends TaggedEventSource with LazyLogging {

  /**
    * This method must be implemented by the consumer to handle the new batch of ModelEvents
    * When a CommitEvent is encountered, the batch is considered complete.
    * The source handler will then invoke the [[EventBatch#consume()]] method
    * and a next batch will be created when new events come in.
    *
    * @param persistenceId The id of the ModelActor that produced the batch of events
    */
  def createBatch(persistenceId: String): T

  def batches: Source[T, NotUsed] = {
    taggedEvents
      .via(new BatchingFlow) // Join all ModelEvents that resulted from a single ModelCommand into a batch
      .filter(_.isDefined).map(_.get) // Is it possible to do this inside the batching flow?
  }

  class BatchingFlow extends GraphStage[FlowShape[ModelEventEnvelope, Option[T]]] {
    val in: Inlet[ModelEventEnvelope] = Inlet[ModelEventEnvelope]("ModelEvent.BatchingFlow.in")
    val out: Outlet[Option[T]] = Outlet[Option[T]]("ModelEvent.BatchingFlow.out")
    val shape: FlowShape[ModelEventEnvelope, Option[T]] = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      val f: ModelEventEnvelope => Option[T] = updateBatch
      setHandler(in, () => push(out, f(grab(in))))
      setHandler(out, () => pull(in))
    }

    import scala.collection.mutable

    // Map with batches per persistenceId that are currently streaming events to us.
    private val currentBatches = new mutable.HashMap[String, T]()

    private def updateBatch(envelope: ModelEventEnvelope): Option[T] = {
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
