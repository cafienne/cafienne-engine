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

package org.cafienne.actormodel.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.SerializedException;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Can be used to return an exception to the sender of the command.
 */
@Manifest
public class CommandFailure extends BaseModelResponse {
    private final Throwable exception;
    private final SerializedException serializedException;
    private final ValueMap exceptionAsJSON;

    /**
     * Create a failure response for the command.
     * The message id of the command will be pasted into the message id of the response.
     * @param command
     * @param failure The reason why the command failed
     */
    public CommandFailure(ModelCommand command, Throwable failure) {
        super(command);
        this.exception = failure;
        this.exceptionAsJSON = Value.convertThrowable(failure);
        this.serializedException = new SerializedException(failure);
    }

    public CommandFailure(ValueMap json) {
        super(json);
        this.exception = null;
        this.exceptionAsJSON = json.readMap(Fields.exception);
        this.serializedException = new SerializedException(exceptionAsJSON);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.exception, exceptionAsJSON);
    }

    /**
     * Returns the underlying exception that caused the command failure.
     * @return
     */
    public Throwable internalException() {
        return exception;
    }

    @Override
    public ValueMap toJson() {
        return exceptionAsJSON;
    }

    /**
     * Returns the exception that caused the command failure
     * @return
     */
    public SerializedException exception() {
        return serializedException;
    }

    @Override
    public String toString() {
        return exceptionAsJSON.toString();
    }
}
