package org.cafienne.actormodel.command;

import akka.actor.ActorPath;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.JSONParseFailure;
import org.cafienne.json.JSONReader;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.actormodel.TenantUserMessage;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.util.Guid;

import java.io.IOException;
import java.io.StringWriter;

public abstract class ModelCommand<T extends ModelActor> implements CafienneSerializable, TenantUserMessage<T> {
    protected final String msgId;
    public final String actorId;
    protected transient T actor;

    /**
     * Store the user that issued the Command.
     */
    final protected TenantUser user;

    protected ModelCommand(TenantUser tenantUser, String actorId) {
        // First, validate actor id to be akka compliant
        if (actorId == null) {
            throw new InvalidCommandException("Actor id cannot be null");
        }
        try {
            ActorPath.validatePathElement(actorId);
        } catch (Throwable t) {
            throw new InvalidCommandException("Invalid actor path " + actorId, t);
        }
        if (tenantUser == null || tenantUser.id() == null || tenantUser.id().trim().isEmpty()) {
            throw new InvalidCommandException("Tenant user cannot be null");
        }
        if (tenantUser.tenant() == null || tenantUser.tenant().isEmpty()) {
            throw new InvalidCommandException("Tenant information is missing for the "+this.getClass().getSimpleName()+" command");
        }
        this.msgId = new Guid().toString();
        this.user = tenantUser;
        this.actorId = actorId;
    }

    protected ModelCommand(ValueMap json) {
        this.msgId = json.raw(Fields.messageId);
        this.actorId = json.raw(Fields.actorId);
        this.user = TenantUser.from(json.with(Fields.user));
    }

    /**
     * Explicit method to be implemented returning the type of the ModelActor handling this message.
     * This is required for the message routing within the CaseSystem
     * @return
     */
    public abstract Class<T> actorClass();

    /**
     * Through this method, the command is made aware of the actor that is handling it.
     * @param actor
     */
    public final void setActor(T actor) {
        this.actor = actor;
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
    public final TenantUser getUser() {
        return user;
    }

    /**
     * Returns a string with the identifier of the actor towards this command must be sent.
     * @return
     */
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
    public abstract <R extends ModelResponse> R process(T modelActor);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeModelCommand(generator);
    }

    protected void writeModelCommand(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.messageId, this.getMessageId());
        writeField(generator, Fields.actorId, this.getActorId());
        writeField(generator, Fields.user, user);
    }

    public String getCommandDescription() {
        return getClass().getSimpleName();
    }

    public String toString() {
        return "Command [" + getCommandDescription() + "]" + super.toString();
    }

    public Value<?> toJson() {
        JsonFactory factory = new JsonFactory();
        StringWriter sw = new StringWriter();
        try {
            JsonGenerator generator = factory.createGenerator(sw);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            writeThisObject(generator);
            generator.close();
            return JSONReader.parse(sw.toString());
        } catch (IOException | JSONParseFailure e) {
            return new ValueMap("message", "Could not make JSON out of command "+getClass().getName(), "exception", Value.convertThrowable(e));
        }
    }
}
