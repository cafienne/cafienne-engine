/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.actormodel.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

public abstract class BaseModelEvent<M extends ModelActor> implements ModelEvent {
    private final ValueMap json;
    
    // Serializable fields
    private final String actorId;
    public final String tenant;
    private final UserIdentity user;
    private final Instant timestamp;

    protected BaseModelEvent(M actor) {
        this.json = new ValueMap();
        this.actorId = actor.getId();
        this.tenant = actor.getTenant();
        this.user = actor.getCurrentUser();
        this.timestamp = actor.getTransactionTimestamp();
    }

    protected BaseModelEvent(ValueMap json) {
        this.json = json;
        ValueMap modelEventJson = json.with(Fields.modelEvent);
        this.actorId = modelEventJson.readString(Fields.actorId);
        this.tenant = modelEventJson.readString(Fields.tenant);
        this.timestamp = modelEventJson.readInstant(Fields.timestamp);
        this.user = modelEventJson.readObject(Fields.user, UserIdentity::deserialize);
    }

    @Override
    public String tenant() {
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
    public final UserIdentity getUser() {
        return user;
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

    @Override
    public final void updateActorState(ModelActor actor) {
        // A very hard cast indeed. But it would be weird if it doesn't work...
        updateState((M) actor);
    }

    protected void writeModelEvent(JsonGenerator generator) throws IOException {
        generator.writeFieldName(Fields.modelEvent.toString());
        generator.writeStartObject();
        writeField(generator, Fields.actorId, this.getActorId());
        writeField(generator, Fields.tenant, this.tenant);
        writeField(generator, Fields.timestamp, this.timestamp);
        writeField(generator, Fields.user, user);
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
