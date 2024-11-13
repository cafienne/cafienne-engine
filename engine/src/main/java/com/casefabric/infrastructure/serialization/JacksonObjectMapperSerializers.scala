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

package com.casefabric.infrastructure.serialization

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, SerializerProvider}
import com.casefabric.json.{JSONReader, ValueMap}

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
    if (parsed.isMap) {
      parsed.asMap
    } else {
      new ValueMap(p.currentName, parsed)
    }
  }
}

