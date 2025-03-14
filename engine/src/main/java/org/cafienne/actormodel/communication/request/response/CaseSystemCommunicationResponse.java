package org.cafienne.actormodel.communication.request.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.CaseSystemCommunicationCommand;
import org.cafienne.actormodel.communication.request.state.RemoteActorState;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.response.ActorLastModified;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

/**
 * Note: the "response" is actually done by sending them as a command back from the "incoming" side of the house
 */
public abstract class CaseSystemCommunicationResponse extends CaseSystemCommunicationCommand implements ModelResponse {
    private Instant lastModified;
    protected CaseSystemCommunicationResponse(ModelCommand command) {
        super(command);
        this.lastModified = command.getActor() != null ? command.getActor().getLastModified() : null;
    }

    protected CaseSystemCommunicationResponse(ValueMap json) {
        super(json);
        this.lastModified = json.readInstant(Fields.lastModified);
    }

    @Override
    public final void validate(ModelActor actor) throws InvalidCommandException {
        // Must have state
    }

    @Override
    public final void process(ModelActor actor) {
        RemoteActorState<?> state = actor.getRemoteActorState(this.actorId);
        if (state == null) {
            // That's weird...
            System.out.println("\n\n!!!!!!!!!!!!!!!!!!!!        We should have state");
        } else {
            process(state);
        }
    }

    protected abstract void process(RemoteActorState<?> state);

    @Override
    public final ModelResponse getResponse() {
        // Sending responses might have cyclic side effects, can't send one therefore.
        return null;
    }

    @Override
    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public ActorLastModified lastModifiedContent() {
        return new ActorLastModified(actorId, lastModified);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeActorCommand(generator);
        writeField(generator, Fields.lastModified, this.lastModified);
    }
}
