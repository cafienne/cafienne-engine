package org.cafienne.akka.actor.serialization;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.cafienne.cmmn.akka.event.CaseDefinitionApplied;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueList;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.infrastructure.json.CafienneJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface AkkaSerializable {
    Logger logger = LoggerFactory.getLogger(AkkaCaseObjectSerializer.class);

    default byte[] toBytes() {
        JsonFactory factory = new JsonFactory();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            JsonGenerator generator = factory.createGenerator(baos);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            this.writeThisObject(generator);
            generator.close();
        } catch (IOException e) {
            throw new RuntimeException("Failure in serialization of an object with type " + this.getClass().getName()+"\n"+e.getMessage(), e);
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
     * Writes this AkkaSerializable object with a start and end object. In between invokes the write method.
     * @param generator
     * @throws IOException
     */
    default void writeThisObject(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        this.write(generator);
        generator.writeEndObject();
    }

    void write(JsonGenerator generator) throws IOException ;

    default void writeListField(JsonGenerator generator, Enum fieldName, Collection<? extends CafienneJson> stringList) throws IOException {
        generator.writeArrayFieldStart(fieldName.toString());
        for (CafienneJson string : stringList) {
            if (string == null) {
                generator.writeNull();
            } else {
                string.toValue().print(generator);
            }
        }
        generator.writeEndArray();
    }

    default void writeField(JsonGenerator generator, Enum fieldName, Collection<String> stringList) throws IOException {
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

    default void writeField(JsonGenerator generator, Enum fieldName, boolean value) throws IOException {
        generator.writeBooleanField(fieldName.toString(), value);
    }

    default void writeField(JsonGenerator generator, Enum fieldName, CMMNElementDefinition value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeFieldName(fieldName.toString());
            value.toJSON().print(generator);
        }
    }

    default void writeField(JsonGenerator generator, Enum fieldName, Value<?> value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeFieldName(fieldName.toString());
            value.print(generator);
        }
    }

    default void writeField(JsonGenerator generator, Enum fieldName, String value) throws IOException {
        generator.writeStringField(fieldName.toString(), String.valueOf(value));
    }

    default void writeField(JsonGenerator generator, Enum fieldName, AkkaSerializable value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeFieldName(fieldName.toString());
            value.write(generator);
        }
    }

    default void writeField(JsonGenerator generator, Enum fieldName, Instant value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeStringField(fieldName.toString(), String.valueOf(value));
        }
    }

    default void writeField(JsonGenerator generator, Enum fieldName, Enum value) throws IOException {
        if (value == null) {
            generator.writeNullField(fieldName.toString());
        } else {
            generator.writeStringField(fieldName.toString(), String.valueOf(value));
        }
    }

    default <T extends Object> T readField(ValueMap json, Enum fieldName) {
        return json.raw(fieldName);
    }

    default <T extends Enum<?>> T readEnum(ValueMap json, Enum fieldName, Class<T> enumClass) {
        return json.getEnum(fieldName, enumClass);
    }

    default Instant readInstant(ValueMap json, Enum fieldName) {
        return json.rawInstant(fieldName);
    }

    default ValueMap readMap(ValueMap json, Enum fieldName) {
        return json.with(fieldName);
    }

    default ValueList readArray(ValueMap json, Enum fieldName) {
        return json.withArray(fieldName);
    }

    default <T> Set<T> readSet(ValueMap json, Enum fieldName) {
        return new HashSet(readArray(json, fieldName).rawList());
    }

    default <T extends CMMNElementDefinition> T readDefinition(ValueMap json, Enum fieldName, Class<T> tClass) {
        return CMMNElementDefinition.fromJSON(readMap(json, fieldName), tClass);
    }
}
