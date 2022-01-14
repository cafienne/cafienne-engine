/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
    private ValueMap exceptionAsJSON;

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
