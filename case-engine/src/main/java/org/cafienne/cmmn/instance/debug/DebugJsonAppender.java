package org.cafienne.cmmn.instance.debug;

import org.cafienne.akka.actor.serialization.json.Value;

@FunctionalInterface
public interface DebugJsonAppender {
    Value info();
}
