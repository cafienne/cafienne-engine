package org.cafienne.infrastructure.cqrs.instance

import org.apache.pekko.persistence.query.Offset
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.json.{CafienneJson, Value, ValueMap}

case class PublicModelEvent(sequenceNr: Long, offset: Offset, event: ModelEvent) extends CafienneJson {
  val eventType: String = event.getClass.getSimpleName
  override def toValue: Value[_] = {
    new ValueMap("nr", sequenceNr, "offset", OffsetRecord("", offset).offsetValue, "type", eventType, "content", event.rawJson)
  }
}
