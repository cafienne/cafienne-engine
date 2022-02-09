package org.cafienne.cmmn.instance.debug;

import org.cafienne.json.Value;

@FunctionalInterface
public interface DebugJsonAppender {
    Value<?> info();
}
