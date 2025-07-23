package org.cafienne.system.router.cluster

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.engine.actorapi.CaseFamily
import org.cafienne.infrastructure.serialization
import org.cafienne.infrastructure.serialization.{CafienneSerializable, Fields}
import org.cafienne.json.ValueMap

@serialization.Manifest
case class MessageEnvelope(family: CaseFamily, actorId: String, message: CafienneSerializable) extends CafienneSerializable {
  override def write(generator: JsonGenerator): Unit = {
    writeField(generator, Fields.family, family)
    writeField(generator, Fields.actorId, actorId)
    writeManifestField(generator, Fields.message, message)
  }
}

object MessageEnvelope {
  def deserialize(json: ValueMap): MessageEnvelope = {
    val family = CaseFamily.deserialize(json.readMap(Fields.family))
    val actorId = json.readString(Fields.actorId)
    val message = json.readManifestField(Fields.message).asInstanceOf[CafienneSerializable]
    MessageEnvelope(family, actorId, message)
  }
}
