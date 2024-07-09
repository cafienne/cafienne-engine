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

package org.cafienne.actormodel.exception;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Wrapper class around Exception that can be serialized
 * and used to read the content of the exception across the actor network.
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
