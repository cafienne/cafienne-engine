/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor.command.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.TenantUserMessage;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.AkkaSerializable;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

/**
 * Interface for creating responses to {@link ModelCommand}
 */
public class ModelResponse implements AkkaSerializable, TenantUserMessage {
    private final String messageId;
    private final String actorId;
    private Instant lastModified;
    private final TenantUser user;
    private final String commandType;

    protected ModelResponse(ModelCommand<?> command) {
        this.messageId = command.getMessageId();
        this.actorId = command.actorId;
        // If a Command never reached the actor (e.g., if CaseSystem routing service ran into an error),
        //  the actor will not be available. Checking that here. Required for CommandFailure.
        this.lastModified = command.getActor() != null ? command.getActor().getLastModified() : null;
        this.lastModified = command.getActor() != null ? command.getActor().getLastModified() : null;
        this.user = command.getUser();
        this.commandType = command.getClass().getName();
    }

    public ModelResponse(ValueMap json) {
        this.messageId = readField(json, Fields.messageId);
        this.actorId = readField(json, Fields.actorId);
        this.lastModified = readInstant(json, Fields.lastModified);
        this.user = TenantUser.from(json.with(Fields.user));
        this.commandType = readField(json, Fields.commandType);
    }

    /**
     * Every {@link ModelCommand} has a message id.
     * Every response to such a command must have the same message id for correlation purposes
     *
     * @return
     */
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

    public ActorLastModified lastModifiedContent() {
        return new ActorLastModified(actorId, getLastModified());
    }

    public TenantUser getUser() {
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
