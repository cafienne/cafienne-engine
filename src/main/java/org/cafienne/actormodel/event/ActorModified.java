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
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.message.IncomingActorMessage;
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
public abstract class ActorModified extends BaseModelEvent<ModelActor> implements CommitEvent {
    public final String source;
    public final Instant lastModified;

    protected ActorModified(ModelActor actor, IncomingActorMessage message) {
        super(actor);
        this.source = message.getClass().getName();
        this.lastModified = actor.getTransactionTimestamp();
    }

    protected ActorModified(ValueMap json) {
        super(json);
        this.lastModified = json.readInstant(Fields.lastModified);
        this.source = json.readString(Fields.source, "unknown message");
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
    public final void updateState(ModelActor actor) {
        actor.setLastModified(lastModified());
    }

    public void writeActorModified(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        writeField(generator, Fields.lastModified, lastModified);
        writeField(generator, Fields.source, source);
    }
}
