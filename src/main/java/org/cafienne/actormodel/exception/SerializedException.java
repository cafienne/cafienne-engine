package org.cafienne.actormodel.exception;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Wrapper class around Exception that can be serialized
 * and used to read the content of the exception across the akka network.
 */
public class SerializedException implements CafienneSerializable {
    private final String className;
    private final String message;
    private SerializedException cause;

    public SerializedException(Throwable t) {
        this.className = t.getClass().getName();
        this.message = t.getMessage();
        if (t.getCause() != null) {
            this.cause = new SerializedException(t.getCause());
        }
    }

    public SerializedException(ValueMap json) {
        this.className = json.readString(Fields.className);
        this.message = json.readString(Fields.message);
        ValueMap jsonCause = json.with(Fields.cause);
        if (!jsonCause.getValue().isEmpty()) {
            this.cause = new SerializedException(jsonCause);
        }
    }

    /**
     * Returns the message of the wrapped exception
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the name of the exception class
     * @return
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the exception class
     * @param <T>
     * @return
     */
    public <T extends Throwable> Class<T> getExceptionClass() {
        try {
            return (Class<T>) Class.forName(getClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("The internal exception class with name "+className+" cannot be found in the classpath", e);
        }
    }

    /**
     * Returns the wrapper of the internal cause; may be null.
     * @return
     */
    public SerializedException getCause() {
        return cause;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.className, className);
        writeField(generator, Fields.message, message);
        writeField(generator, Fields.cause, cause);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder(className + ": " + message);
        if (cause != null) {
            string.append("\n\t" + cause);
        }
        return string.toString();
    }
}
