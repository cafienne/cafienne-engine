package org.cafienne.actormodel.communication.outgoing.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.outgoing.RemoteActorState;
import org.cafienne.actormodel.exception.SerializedException;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ActorRequestFailed extends ModelActorSystemResponse {
    private final Throwable exception;
    public final SerializedException serializedException;
    public final ValueMap exceptionAsJSON;

    public ActorRequestFailed(ModelCommand command, Throwable failure) {
        super(command);
        this.exception = failure;
        this.exceptionAsJSON = Value.convertThrowable(failure);
        this.serializedException = new SerializedException(failure);
    }

    public ActorRequestFailed(ValueMap json) {
        super(json);
        this.exception = null;
        this.exceptionAsJSON = json.readMap(Fields.exception);
        this.serializedException = new SerializedException(exceptionAsJSON);
    }

    @Override
    protected void process(RemoteActorState<?> state) {
        state.registerDelivery(this);
    }

    @Override
    public String getCommandDescription() {
        return "Failed[" + command.getDescription() +"]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeActorCommand(generator);
        writeField(generator, Fields.exception, exceptionAsJSON);
    }
}