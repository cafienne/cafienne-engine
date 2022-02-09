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
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

/**
 * Basic implementation for ModelResponse to reply to a {@link ModelCommand}
 */
public abstract class BaseModelResponse implements ModelResponse {
    private final String messageId;
    private final String actorId;
    private Instant lastModified;
    private final UserIdentity user;
    private final String commandType;

    protected BaseModelResponse(ModelCommand command) {
        this.messageId = command.getMessageId();
        this.actorId = command.actorId();
        // If a Command never reached the actor (e.g., if CaseSystem routing service ran into an error),
        //  the actor will not be available. Checking that here. Required for CommandFailure.
        this.lastModified = command.getActor() != null ? command.getActor().getLastModified() : null;
        this.user = command.getUser();
        this.commandType = command.getClass().getName();
    }

    protected BaseModelResponse(ValueMap json) {
        this.messageId = json.readString(Fields.messageId);
        this.actorId = json.readString(Fields.actorId);
        this.lastModified = json.readInstant(Fields.lastModified);
        this.user = json.readObject(Fields.user, UserIdentity::deserialize);
        this.commandType = json.readString(Fields.commandType);
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    /**
     * Returns the actor to which a command was sent that led to this response
     * @return
     */
    public String getActorId() {
        return actorId;
    }

    /**
     * Returns the type of command that gave cause to this response
     *
     * @return
     */
    public String getCommandType() {
        return commandType;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    @Override
    public ActorLastModified lastModifiedContent() {
        return new ActorLastModified(actorId, getLastModified());
    }

    public UserIdentity getUser() {
        return user;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.messageId, this.getMessageId());
        writeField(generator, Fields.actorId, actorId);
        writeField(generator, Fields.commandType, commandType);
        writeField(generator, Fields.lastModified, this.getLastModified());
        writeField(generator, Fields.user, user);
    }

    @Override
    public String toString() {
        return asString();
    }
}
