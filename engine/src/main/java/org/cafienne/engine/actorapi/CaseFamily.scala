package org.cafienne.engine.actorapi

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

case class CaseFamily(identifier: String) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.identifier, identifier)

  override def toString: String = identifier
}

object CaseFamily {
  def deserialize(json: ValueMap): CaseFamily = CaseFamily(json.readString(Fields.identifier))
}
