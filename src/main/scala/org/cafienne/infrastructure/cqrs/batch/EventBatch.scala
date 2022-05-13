package org.cafienne.infrastructure.cqrs.batch

import akka.Done
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

trait EventBatch {
  val persistenceId: String

  val events = new ListBuffer[ModelEventEnvelope]()

  lazy val commitEvent: CommitEvent = events.last.event.asInstanceOf[CommitEvent]

  def addEvent(envelope: ModelEventEnvelope): Unit = {
    events += envelope
  }

  /**
    * Method that is invoked after a CommitEvent is added to the batch.
    * @return
    */
  def consume(): Future[Done]
}
