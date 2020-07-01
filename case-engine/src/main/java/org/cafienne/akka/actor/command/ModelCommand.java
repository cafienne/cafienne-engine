package org.cafienne.akka.actor.command;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.TenantUserMessage;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.serialization.AkkaSerializable;
import org.cafienne.cmmn.instance.casefile.JSONParseFailure;
import org.cafienne.cmmn.instance.casefile.JSONReader;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.util.Guid;

import java.io.IOException;
import java.io.StringWriter;

public abstract class ModelCommand<T extends ModelActor> implements AkkaSerializable, TenantUserMessage {
    protected final String msgId;
    public final String actorId;
    protected transient T actor;

    /**
     * Store the user that issued the Command.
     */
    final protected TenantUser user;

    protected ModelCommand(TenantUser tenantUser, String actorId) {
        this.msgId = new Guid().toString();
        if (tenantUser == null) {
            throw new SecurityException("TenantUser cannot be null");
        }

        // TTR: must be validated before command is sent
        if (tenantUser.tenant() == null || tenantUser.tenant().isEmpty()) {
            throw new SecurityException("Tenant information is missing for the "+this.getClass().getSimpleName()+" command");
        }
        if (actorId == null) {
            throw new SecurityException("Actor id cannot be null");
        }
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
