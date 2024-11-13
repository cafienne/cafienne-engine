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

package com.casefabric.infrastructure.serialization;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.instance.Path;
import com.casefabric.json.CaseFabricJson;
import com.casefabric.json.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;

public interface CaseFabricSerializable {
    Logger logger = LoggerFactory.getLogger(CaseFabricSerializer.class);

    default byte[] toBytes() {
        JsonFactory factory = new JsonFactory();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            JsonGenerator generator = factory.createGenerator(baos);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            this.writeThisObject(generator);
            generator.close();
        } catch (IOException e) {
            throw new RuntimeException("Failure in serialization of an object with type " + this.getClass().getName() + "\n" + e.getMessage(), e);
        } catch (Throwable t) {
            logger.error("Failed to serialize an object of type " + this.getClass().getName(), t);
            throw t;
        }
        return baos.toByteArray();
    }

    default String asString() {
        return new String(toBytes());
    }

    /**
     * Writes this CaseFabricSerializable object with a start and end object. In between invokes the write method.
     *
     * @param generator
     * @throws IOException
     */
    default void writeThisObject(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        this.write(generator);
        generator.writeEndObject();
    }

    void write(JsonGenerator generator) throws IOException;

    default void writeListField(JsonGenerator generator, Object fieldName, Collection<? extends CaseFabricJson> list) throws IOException {
        generator.writeArrayFieldStart(String.valueOf(fieldName));
        for (CaseFabricJson string : list) {
            if (string == null) {
                generator.writeNull();
            } else {
                string.toValue().print(generator);
            }
        }
        generator.writeEndArray();
    }

    default void writeField(JsonGenerator generator, Object fieldName, scala.collection.Iterable<String> list) throws IOException {
        writeField(generator, fieldName, scala.jdk.CollectionConverters.IterableHasAsJava(list).asJavaCollection());
    }

    default void writeField(JsonGenerator generator, Object fieldName, Collection<String> stringList) throws IOException {
        generator.writeArrayFieldStart(String.valueOf(fieldName));
        for (String string : stringList) {
            if (string == null) {
                generator.writeNull();
            } else {
                generator.writeString(string);
            }
        }
        generator.writeEndArray();
    }

    default void writeField(JsonGenerator generator, Object fieldName, boolean value) throws IOException {
        generator.writeBooleanField(String.valueOf(fieldName), value);
    }

    default void writeField(JsonGenerator generator, Object fieldName, CMMNElementDefinition value) throws IOException {
        if (value == null) {
            generator.writeNullField(String.valueOf(fieldName));
        } else {
            generator.writeFieldName(String.valueOf(fieldName));
            value.toJSON().print(generator);
        }
    }

    default void writeField(JsonGenerator generator, Object fieldName, Value<?> value) throws IOException {
        if (value == null) {
            generator.writeNullField(String.valueOf(fieldName));
        } else {
            generator.writeFieldName(String.valueOf(fieldName));
            value.print(generator);
        }
    }

    default void writeField(JsonGenerator generator, Object fieldName, String value) throws IOException {
        if (value == null) {
            generator.writeNullField(String.valueOf(fieldName));
        } else {
            generator.writeStringField(String.valueOf(fieldName), value);
        }
    }

    default void writeField(JsonGenerator generator, Object fieldName, Path value) throws IOException {
        if (value == null) {
            generator.writeNullField(String.valueOf(fieldName));
        } else {
            generator.writeStringField(String.valueOf(fieldName), String.valueOf(value));
        }
    }

    default void writeField(JsonGenerator generator, Object fieldName, Instant value) throws IOException {
        if (value == null) {
            generator.writeNullField(String.valueOf(fieldName));
        } else {
            generator.writeStringField(String.valueOf(fieldName), String.valueOf(value));
        }
    }

    default void writeField(JsonGenerator generator, Object fieldName, Enum<?> value) throws IOException {
        if (value == null) {
            generator.writeNullField(String.valueOf(fieldName));
        } else {
            generator.writeStringField(String.valueOf(fieldName), String.valueOf(value));
        }
    }

    default void writeField(JsonGenerator generator, Object fieldName, CaseFabricJson value) throws IOException {
        if (value == null) {
            generator.writeNullField(String.valueOf(fieldName));
        } else {
            generator.writeFieldName(String.valueOf(fieldName));
            value.write(generator);
        }
    }

    default void writeField(JsonGenerator generator, Object fieldName, CaseFabricSerializable value) throws IOException {
        if (value == null) {
            generator.writeNullField(String.valueOf(fieldName));
        } else {
            generator.writeFieldName(String.valueOf(fieldName));
            value.writeThisObject(generator);
        }
    }
}
