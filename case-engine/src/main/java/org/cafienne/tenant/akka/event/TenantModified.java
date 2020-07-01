package org.cafienne.tenant.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.event.TransactionEvent;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;
import java.time.Instant;

/**
 * Event that is published after an {@link org.cafienne.tenant.akka.command.TenantCommand} has been fully handled by a {@link TenantActor} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class TenantModified extends TenantEvent implements TransactionEvent<TenantActor> {
    private final Instant lastModified;

    public TenantModified(TenantActor actor, Instant lastModified) {
        super(actor);
        this.lastModified = lastModified;
    }

    public TenantModified(ValueMap value) {
        super(value);
        this.lastModified = value.rawInstant(Fields.lastModified);
    }

    /**
     * Returns the moment at which the case was last modified
     * @return
     */
    public Instant lastModified() {
        return lastModified;
    }


    @Override
    public String toString() {
        return "Modified process task [" + getActorId() + "] at " + lastModified;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        writeField(generator, Fields.lastModified, lastModified);
    }

    @Override
    public void updateState(TenantActor tenant) {
        tenant.updateState(this);
    }
}
