package org.cafienne.actormodel.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

/**
 * Interface for a ModelActor to use to express that an event completes the handling of a certain
 * incoming message that has lead to state changes in the actor.
 *
 * @param <M>
 */
public abstract class ActorModified<M extends ModelActor<?,?>> extends BaseModelEvent<M> implements CommitEvent {
    public final String source;
    public final Instant lastModified;

    protected ActorModified(ModelCommand<M> command) {
        super(command.getActor());
        this.source = command.getClass().getName();
        this.lastModified = command.getActor().getTransactionTimestamp();
    }

    protected ActorModified(ValueMap json) {
        super(json);
        this.lastModified = json.rawInstant(Fields.lastModified);
        this.source = readField(json, Fields.source, "unknown message");
    }

    public Instant lastModified() {
        return lastModified;
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + " upon " + source;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeActorModified(generator);
    }

    @Override
    public void updateState(M actor) {
        actor.setLastModified(lastModified());
    }

    public void writeActorModified(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        writeField(generator, Fields.lastModified, lastModified);
        writeField(generator, Fields.source, source);
    }
}
