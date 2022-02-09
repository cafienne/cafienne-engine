package org.cafienne.infrastructure.cqrs

import akka.persistence.query.{EventEnvelope, Offset}
import org.cafienne.actormodel.event.ModelEvent

case class ModelEventEnvelope(envelope: EventEnvelope) {
  lazy val offset: Offset = envelope.offset
  lazy val persistenceId: String = envelope.persistenceId
  lazy val sequenceNr: Long = envelope.sequenceNr
  lazy val event: ModelEvent = envelope.event.asInstanceOf[ModelEvent]
}

