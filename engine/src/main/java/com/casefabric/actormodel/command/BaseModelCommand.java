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

package com.casefabric.actormodel.command;

import org.apache.pekko.actor.ActorPath;
import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.casefabric.actormodel.ModelActor;
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.UserIdentity;
import com.casefabric.actormodel.response.ModelResponse;
import com.casefabric.cmmn.actorapi.response.CaseResponse;
import com.casefabric.infrastructure.serialization.CaseFabricSerializer;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.JSONParseFailure;
import com.casefabric.json.JSONReader;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;
import com.casefabric.util.Guid;

import java.io.IOException;
import java.io.StringWriter;

public abstract class BaseModelCommand<T extends ModelActor, U extends UserIdentity> implements ModelCommand {
    protected final String msgId;
    public final String actorId;
    public ActorRef sender;
    protected transient T actor;
    private ModelResponse response;

    /**
     * Store the user that issued the Command.
     */
    private final U user;

    protected BaseModelCommand(U user, String actorId) {
        // First, validate actor id
        if (actorId == null) {
            throw new InvalidCommandException("Actor id cannot be null");
        }
        try {
            ActorPath.validatePathElement(actorId);
        } catch (Throwable t) {
            throw new InvalidCommandException("Invalid actor path " + actorId, t);
        }
        if (user == null || user.id() == null || user.id().trim().isEmpty()) {
            throw new InvalidCommandException("Tenant user cannot be null");
        }
        this.msgId = new Guid().toString();
        this.user = user;
        this.actorId = actorId;
    }

    protected BaseModelCommand(ValueMap json) {
        this.msgId = json.readString(Fields.messageId);
        this.actorId = json.readString(Fields.actorId);
        this.user = readUser(json.with(Fields.user));
    }

    /**
     * Model actor specific command to is responsible for deserializing user to appropriate type.
     * @param json
     * @return
     */
    protected abstract U readUser(ValueMap json);

    /**
     * Through this method, the command is made aware of the actor that is handling it.
     * @param actor
     */
    @Override
    public final void setActor(ModelActor actor) {
        this.actor = (T) actor;
        this.sender = actor.getSender();
    }

    /**
     * Returns information about the sender of this command.
     * Can be used to independently send replies.
     * Independently means: independent of the command processing logic.
     * Note: if the response field of the command is filled, the framework will also send a response to the sender.
     */
    public ActorRef getSender() {
        return sender;
    }

    @Override
    public final void validateCommand(ModelActor actor) {
        validate((T) actor);
    }

    @Override
    public final void processCommand(ModelActor actor) {
        process((T) actor);
    }

    @Override
    public ModelResponse getResponse() {
        return response;
    }

    public void setResponse(ModelResponse response) {
        this.response = response;
    }

    @Override
    public boolean hasResponse() {
        return response != null;
    }

    protected boolean hasNoResponse() {
        return response == null;
    }

    /**
     * Note: this method will only return a sensible value when it is invoked from within the command handling context.
     * It is intended for command handlers to have more metadata when creating a ModelResponse.
     * @return
     */
    public T getActor() {
        return actor;
    }

    /**
     * Returns the user context for this command.
     *
     * @return
     */
    @Override
    public final U getUser() {
        return user;
    }

    /**
     * Returns a string with the identifier of the actor towards this command must be sent.
     * @return
     */
    @Override
    public final String getActorId() {
        return actorId;
    }

    /**
     * Returns the correlation id of this command, that can be used to relate a {@link CaseResponse} back to this
     * original command.
     *
     * @return
     */
    public String getMessageId() {
        return msgId;
    }

    /**
     * Before the Model Actor starts processing the command, it will first ask to validate the command.
     * Implementations may override this method to implement their own validation logic.
     * Implementations may throw the {@link InvalidCommandException} if they encounter a validation error
     *
     * @param modelActor
     * @throws InvalidCommandException If the command is invalid
     */
    public abstract void validate(T modelActor) throws InvalidCommandException;

    /**
     * Method to be implemented to handle the command.
     * @param modelActor
     * @return
     */
    public abstract void process(T modelActor);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeModelCommand(generator);
    }

    protected void writeModelCommand(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.messageId, this.getMessageId());
        writeField(generator, Fields.actorId, this.getActorId());
        writeField(generator, Fields.user, user);
    }

    @Override
    public String getCommandDescription() {
        return getClass().getSimpleName();
    }

    public String toString() {
        return "Command [" + getCommandDescription() + "]" + super.toString();
    }
}
