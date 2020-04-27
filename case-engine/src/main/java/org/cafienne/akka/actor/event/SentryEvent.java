package org.cafienne.akka.actor.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;

@Manifest
public class SentryEvent extends DebugEvent {
    public SentryEvent(ValueMap json) {
        super(json);
    }
}
