package org.cafienne.processtask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.TransactionEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;
import java.time.Instant;

/**
 * Event that is published after an {@link org.cafienne.processtask.actorapi.command.ProcessCommand} has been fully handled by a {@link ProcessTaskActor} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class ProcessModified extends ProcessInstanceEvent implements TransactionEvent<ProcessTaskActor> {
    private final Instant lastModified;
    private String source;

    public ProcessModified(ProcessTaskActor actor, Instant lastModified) {
        super(actor);
        this.lastModified = lastModified;
    }

    public ProcessModified(ValueMap json) {
        super(json);
        this.lastModified = json.rawInstant(Fields.lastModified);
        this.source = readField(json, Fields.source, "unknown message");
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.setLastModified(lastModified);
    }

    /**
     * Returns the moment at which the case was last modified
     * @return
     */
    public Instant lastModified() {
        return lastModified;
    }

    @Override
    public void setCause(String source) {
        this.source = source;
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + " upon " + source;
    }

    @Override
    public String toString() {
        return "Modified process task [" + getActorId() + "] at " + lastModified;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        writeField(generator, Fields.lastModified, lastModified);
        writeField(generator, Fields.source, source);
    }
}
