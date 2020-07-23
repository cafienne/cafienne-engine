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
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.StringValue;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;

import java.io.IOException;

/**
 * DebugEvent
 */
@Manifest
public class DebugEvent extends BaseModelEvent {
    private final ValueMap messages;

    public DebugEvent(ModelActor modelActor) {
        super(modelActor);
        this.messages = new ValueMap();
    }

    public DebugEvent(ValueMap json) {
        super(json);
        this.messages = readMap(json, Fields.messages);
    }

    public void addMessage(String msg) {
        add(new StringValue(msg));
    }

    public void addMessage(Value json) {
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
