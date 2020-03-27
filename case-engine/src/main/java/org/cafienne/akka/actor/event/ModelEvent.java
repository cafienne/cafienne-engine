/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.TenantUserMessage;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.AkkaSerializable;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

public abstract class ModelEvent<M extends ModelActor> implements AkkaSerializable, TenantUserMessage {
    public static final String TAG = "cafienne";
    private final ValueMap json;

    // Serializable fields
    private final String actorId;
    public final String tenant;
    private final TenantUser tenantUser;

    /**
     * During recovery, Actor is set in call to {@link ModelEvent#recover(ModelActor)}
     * So during {@link ModelEvent#updateState(ModelActor)} it can be used.
     */
    protected transient M actor;

    public enum Fields {
        tenant, actorId, user, modelEvent
    }

    protected ModelEvent(M actor) {
        this.json = new ValueMap();
        this.actorId = actor.getId();
        this.tenant = actor.getTenant();
        this.tenantUser = actor.getCurrentUser();
        this.actor = actor;
    }

    protected ModelEvent(ValueMap json) {
        this.json = json;
        json = json.with(Fields.modelEvent);
        this.actorId = readField(json, Fields.actorId);
        this.tenant = readField(json, Fields.tenant);
        this.tenantUser = TenantUser.from(json.with(Fields.user));
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
     * UpdateState will be invoked when an event is added or recovered.
     * @param actor
     */
    public abstract void updateState(M actor);

    /**
     * Events can implement behavior to be run after the event has updated it's state.
     * This method will only be invoked when the model actor is not running in recovery mode.
     */
    public void runBehavior() {
        // Default behavior is none
    }

    /**
     * Internal framework method
     * @param actor
     */
    public final void recover(M actor) {
        this.actor = actor;
        this.updateState(actor);
    }

    protected void writeModelEvent(JsonGenerator generator) throws IOException {
        generator.writeFieldName(Fields.modelEvent.toString());
        generator.writeStartObject();
        writeField(generator, Fields.actorId, this.getActorId());
        writeField(generator, Fields.tenant, this.tenant);
        generator.writeFieldName(Fields.user.toString());
        tenantUser.write(generator);
        generator.writeEndObject();
    }
}
