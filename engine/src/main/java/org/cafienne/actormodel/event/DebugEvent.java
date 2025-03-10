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
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.StringValue;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * DebugEvent
 */
@Manifest
public class DebugEvent extends CaseSystemEvent {
    private final ValueMap messages;

    public DebugEvent(ModelActor modelActor) {
        super(modelActor);
        this.messages = new ValueMap();
    }

    public DebugEvent(ValueMap json) {
        super(json);
        this.messages = json.readMap(Fields.messages);
    }

    public void addMessage(String msg) {
        for (String s : msg.split("\n")) {
            add(new StringValue(s));
        }
    }

    public void addMessage(Value<?> json) {
        add(json);
    }

    public void addMessage(Throwable exception) {
        add(Value.convertThrowable(exception));
    }

    private void add(Value<?> value) {
        messages.put("" + messages.getValue().size(), value);
    }

    @Override
    public String toString() {
        return messages.toString();
    }

    @Override
    public void updateState(ModelActor actor) {
        // nothing to update
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        writeField(generator, Fields.messages, messages);
    }
}
