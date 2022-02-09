package org.cafienne.actormodel.event;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class SentryEvent extends DebugEvent {
    public SentryEvent(ValueMap json) {
        super(json);
    }
}
