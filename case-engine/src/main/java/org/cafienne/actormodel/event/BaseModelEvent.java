package org.cafienne.actormodel.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

public abstract class BaseModelEvent<M extends ModelActor<?,?>> implements ModelEvent<M> {
    private final ValueMap json;

    // Serializable fields
    private final String actorId;
    public final String tenant;
    private final TenantUser tenantUser;
    private final Instant timestamp;

    /**
     * During recovery, Actor is set in call to {@link ModelEvent#recover(ModelActor)}
     * So during {@link ModelEvent#updateState(ModelActor)} it can be used.
     */
    protected transient M actor;

    protected BaseModelEvent(M actor) {
        this.json = new ValueMap();
        this.actorId = actor.getId();
        this.tenant = actor.getTenant();
        this.tenantUser = actor.getCurrentUser();
        this.actor = actor;
        this.timestamp = actor.getTransactionTimestamp();
    }

    protected BaseModelEvent(ValueMap json) {
        this.json = json;
        ValueMap modelEventJson = json.with(Fields.modelEvent);
        this.actorId = readField(modelEventJson, Fields.actorId);
        this.tenant = readField(modelEventJson, Fields.tenant);
        this.timestamp = readInstant(modelEventJson, Fields.timestamp);
        this.tenantUser = TenantUser.from(modelEventJson.with(Fields.user));
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    /**
     * Returns the raw json used to (de)serialize this event
     * This method cannot be invoked upon first event creation.
     *
     * @return
     */
    public final ValueMap rawJson() {
        return this.json;
    }

    /**
     * Returns the identifier of the ModelActor that generated this event.
     * Is the same as the persistence id of the underlying Akka Actor.
     *
     * @return
     */
    public final String getActorId() {
        return this.actorId;
    }

    /**
     * Returns the complete context of the user that caused the event to happen
     *
     * @return
     */
    public final TenantUser getUser() {
        return tenantUser;
    }

    /**
     * Returns the event timestamp
     * @return
     */
    public final Instant getTimestamp() {
        return timestamp;
    }

    /**
     * UpdateState will be invoked when an event is added or recovered.
     *
     * @param actor
     */
    public abstract void updateState(M actor);

    /**
     * Internal framework method
     *
     * @param actor
     */
    public final void recover(M actor) {
        this.actor = actor;
        if (logger.isDebugEnabled()) {
            logger.debug("Recovery in " + actor.getDescription() + "[" + actor.lastSequenceNr() + "]: " + timestamp + " - " + this.getDescription());
        }
        this.updateState(actor);
    }

    protected void writeModelEvent(JsonGenerator generator) throws IOException {
        generator.writeFieldName(Fields.modelEvent.toString());
        generator.writeStartObject();
        writeField(generator, Fields.actorId, this.getActorId());
        writeField(generator, Fields.tenant, this.tenant);
        writeField(generator, Fields.timestamp, this.timestamp);
        writeField(generator, Fields.user, tenantUser);
        generator.writeEndObject();
    }

    /**
     * Override this method to make a description of the event that is based on it's content.
     * This method is invoked from toString().
     *
     * @return
     */
    public String getDescription() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
