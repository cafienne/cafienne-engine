package org.cafienne.actormodel.event;

import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.actormodel.serialization.json.ValueMap;

@Manifest
public class SentryEvent extends DebugEvent {
    public SentryEvent(ValueMap json) {
        super(json);
    }
}
