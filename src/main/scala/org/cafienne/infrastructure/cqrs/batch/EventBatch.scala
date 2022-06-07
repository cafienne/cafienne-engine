package org.cafienne.infrastructure.cqrs.batch

import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope

import scala.collection.mutable.ListBuffer

trait EventBatch {
  val persistenceId: String

  val events = new ListBuffer[ModelEventEnvelope]()

  lazy val commitEvent: CommitEvent = events.last.event.asInstanceOf[CommitEvent]

  def addEvent(envelope: ModelEventEnvelope): Unit = {
    events += envelope
  }
}
