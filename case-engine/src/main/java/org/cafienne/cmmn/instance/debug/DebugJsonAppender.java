package org.cafienne.cmmn.instance.debug;

import org.cafienne.actormodel.serialization.json.Value;

@FunctionalInterface
public interface DebugJsonAppender {
    Value info();
}
