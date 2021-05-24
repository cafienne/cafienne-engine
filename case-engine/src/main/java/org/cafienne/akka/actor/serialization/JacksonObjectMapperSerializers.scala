package org.cafienne.akka.actor.serialization

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import org.cafienne.akka.actor.serialization.json.{JSONReader, ValueMap}

class ValueMapJacksonSerializer(c: Class[ValueMap]) extends StdSerializer[ValueMap](c) {

  def this() = this(null)

  override def serialize(value: ValueMap, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    value.print(gen)
  }
}

class ValueMapJacksonDeserializer(c: Class[_]) extends StdDeserializer[ValueMap](c) {

  def this() = this(null)

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): ValueMap = {
    val parsed = JSONReader.read(p, null)
    if (parsed.isInstanceOf[ValueMap]) {
      parsed.asInstanceOf[ValueMap]
    } else {
      new ValueMap(p.getCurrentName, parsed)
    }
  }
}

