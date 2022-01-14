/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.actormodel.response;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.IncomingActorMessage;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

/**
 * Basic implementation for ModelResponse to reply to a {@link ModelCommand}
 */
public abstract class BaseModelResponse implements ModelResponse {
    /**
     * Recipient is assigned during construction of the response message.
     * It is the current value of sender() in the actor.
     */
    private final transient ActorRef recipient;
    private final String messageId;
    private final String actorId;
    private Instant lastModified;
    private final UserIdentity user;
    private final String commandType;

    protected BaseModelResponse(ModelCommand<?, ?> command) {
        // Make sure that we capture the recipient during creation.
        //  This is required since sending the response is done after events are persisted,
        //  and that in itself may or may not happen asynchronously, and if it is happening
        //  asynchronously, then the sender() value of the Actor may have been overwritten with the sender
        //  of the next message handled by the actor already.
        this.recipient = command.getActor().sender();
        this.messageId = command.getMessageId();
        this.actorId = command.actorId;
        // If a Command never reached the actor (e.g., if CaseSystem routing service ran into an error),
        //  the actor will not be available. Checking that here. Required for CommandFailure.
        this.lastModified = command.getActor() != null ? command.getActor().getLastModified() : null;
        this.user = command.getUser();
        this.commandType = command.getClass().getName();
    }

    protected BaseModelResponse(ValueMap json) {
        this.recipient = null;
        this.messageId = json.readString(Fields.messageId);
        this.actorId = json.readString(Fields.actorId);
        this.lastModified = json.readInstant(Fields.lastModified);
        this.user = json.readObject(Fields.user, UserIdentity::deserialize);
        this.commandType = json.readString(Fields.commandType);
    }

    @Override
    public ActorRef getRecipient() {
        return recipient;
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
