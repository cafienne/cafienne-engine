package org.cafienne.infrastructure.cqrs

import akka.persistence.query.EventEnvelope
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.serialization.{DeserializationFailure, UnrecognizedManifest}

trait ModelEventFilter extends LazyLogging {
  def validateModelEvents(element: EventEnvelope): Boolean = {
    element match {
      case EventEnvelope(_, _, _, _: ModelEvent) => true
      case _ =>
        val offset = element.offset
        val persistenceId = element.persistenceId
        val sequenceNr = element.sequenceNr
        val eventDescriptor = s"Encountered unexpected event with offset=$offset, persistenceId=$persistenceId, sequenceNumber=$sequenceNr"
        element.event match {
          case evt: DeserializationFailure =>
            logger.error("Ignoring event of type '" + evt.manifest + s"' with invalid contents. It could not be deserialized. $eventDescriptor", evt.exception)
            logger.whenDebugEnabled(logger.debug("Event blob: " + new String(evt.blob)))
          case evt: UnrecognizedManifest =>
            logger.error("Ignoring unrecognized event of type '" + evt.manifest + s"'. Event type is probably deprecated. $eventDescriptor")
            logger.whenDebugEnabled(logger.debug("Event contents: " + new String(evt.blob)))
          case other =>
            logger.error("Ignoring unknown event of type '" + other.getClass.getName + s"'. Event type is perhaps created through some other product. $eventDescriptor")
        }
        false
    }
  }
}
