package org.cafienne.infrastructure.serialization;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.json.CafienneJson;
import org.cafienne.json.Value;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface CafienneSerializable {
    Logger logger = LoggerFactory.getLogger(CafienneSerializer.class);

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
     * Writes this CafienneSerializable object with a start and end object. In between invokes the write method.
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

    default void writeListField(JsonGenerator generator, Fields fieldName, Collection<? extends CafienneJson> list) throws IOException {
        generator.writeArrayFieldStart(fieldName.toString());
        for (CafienneJson string : list) {
            if (string == null) {
                generator.writeNull();
            } else {
                string.toValue().print(generator);
            }
        }
        generator.writeEndArray();
    }

    default void writeField(JsonGenerator generator, Fields fieldName, Collection<String> stringList) throws IOException {
        generator.writeArrayFieldStart(fieldName.toString());
        for (String string : stringList) {
            if (string == null) {
                generator.writeNull();
            } else {
                generator.writeString(string);
            }
        }
        generator.writeEndArray();
    }

    default void writeField(JsonGenerator generator, Fields fieldName, boolean value) throws IOException {
        generator.writeBooleanField(fieldName.toString(), value);
    }

    default void writeField(JsonGenerator generator, Fields fieldName, CMMNElementDefinition value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeFieldName(fieldName.toString());
            value.toJSON().print(generator);
        }
    }

    default void writeField(JsonGenerator generator, Fields fieldName, Value<?> value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeFieldName(fieldName.toString());
            value.print(generator);
        }
    }

    default void writeField(JsonGenerator generator, Fields fieldName, String value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeStringField(fieldName.toString(), value);
        }
    }

    default void writeField(JsonGenerator generator, Fields fieldName, Path value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeStringField(fieldName.toString(), String.valueOf(value));
        }
    }

    default void writeField(JsonGenerator generator, Fields fieldName, Instant value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeStringField(fieldName.toString(), String.valueOf(value));
        }
    }

    default void writeField(JsonGenerator generator, Fields fieldName, Enum value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeStringField(fieldName.toString(), String.valueOf(value));
        }
    }

    default void writeField(JsonGenerator generator, Fields fieldName, CafienneJson value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeFieldName(fieldName.toString());
            value.write(generator);
        }
    }

    default void writeField(JsonGenerator generator, Fields fieldName, CafienneSerializable value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeFieldName(fieldName.toString());
            value.writeThisObject(generator);
        }
    }

    default <T extends Object> T readField(ValueMap json, Fields fieldName) {
        return json.raw(fieldName);
    }

    default <T extends Object> T readField(ValueMap json, Fields fieldName, T defaultValue) {
        if (json.has(fieldName)) {
            return readField(json, fieldName);
        } else {
            return defaultValue;
        }
    }

    default <T extends Enum<?>> T readEnum(ValueMap json, Fields fieldName, Class<T> enumClass) {
        return json.getEnum(fieldName, enumClass);
    }

    default Instant readInstant(ValueMap json, Fields fieldName) {
        return json.rawInstant(fieldName);
    }

    default ValueMap readMap(ValueMap json, Fields fieldName) {
        return json.with(fieldName);
    }

    default ValueList readArray(ValueMap json, Fields fieldName) {
        return json.withArray(fieldName);
    }

    default <T> Set<T> readSet(ValueMap json, Fields fieldName) {
        return new HashSet<>(readArray(json, fieldName).rawList());
    }

    default <T extends CMMNElementDefinition> T readDefinition(ValueMap json, Fields fieldName, Class<T> tClass) {
        return CMMNElementDefinition.fromJSON(this.getClass().getName(), readMap(json, fieldName), tClass);
    }

    default Path readPath(ValueMap json, Fields fieldName) {
        return new Path((String) json.raw(fieldName));
    }
}
