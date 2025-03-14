package org.cafienne.actormodel.communication.reply.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.reply.command.RunActorRequest;
import org.cafienne.actormodel.event.CommitEvent;
import org.cafienne.actormodel.exception.SerializedException;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ActorRequestFailed extends ModelActorRequestEvent implements CommitEvent {
    private final Throwable exception;
    public final SerializedException serializedException;
    public final ValueMap exceptionAsJSON;

    public ActorRequestFailed(RunActorRequest request, Throwable failure) {
        super(request.command, request.sourceActorId);
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
    public void write(JsonGenerator generator) throws IOException {
        super.writeIncomingRequestEvent(generator);
        writeField(generator, Fields.exception, exceptionAsJSON);
    }

}
