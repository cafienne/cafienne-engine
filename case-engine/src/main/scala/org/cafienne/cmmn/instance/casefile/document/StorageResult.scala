package org.cafienne.cmmn.instance.casefile.document

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.akka.actor.serialization.json.{Value, ValueMap}
import org.cafienne.akka.actor.serialization.{CafienneSerializable, DeserializationError, Fields}

case class StorageResult(identifier: DocumentIdentifier, contentType: String) extends CafienneSerializable {
  override def write(generator: JsonGenerator): Unit = {
    generator.writeStartObject()
    writeField(generator, Fields.identifier, identifier.toString)
    writeField(generator, Fields.contentType, contentType)
    generator.writeEndObject()
  }
}

object StorageResult {
  def deserialize(json: Value[_]): StorageResult = {
    if (json.isInstanceOf[ValueMap]) {
      val identifier: String = json.asInstanceOf[ValueMap].raw(Fields.identifier).asInstanceOf[String]
      val contentType: String = json.asInstanceOf[ValueMap].raw(Fields.contentType).asInstanceOf[String]
      StorageResult(DocumentIdentifier(identifier), contentType)
    }
    else {
      throw new DeserializationError("Deserializing a StorageResult requires a json object structure instead of a " + json.getClass.getName)
    }
  }
}