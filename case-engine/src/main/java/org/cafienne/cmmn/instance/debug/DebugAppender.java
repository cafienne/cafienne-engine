package org.cafienne.cmmn.instance.debug;

import org.cafienne.cmmn.akka.event.debug.DebugEvent;

@FunctionalInterface
public interface DebugAppender<T extends DebugEvent> {
    void add(T event);
}
