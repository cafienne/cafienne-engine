package org.cafienne.cmmn.instance.debug;

import org.cafienne.cmmn.instance.casefile.Value;

@FunctionalInterface
public interface DebugJsonAppender {
    Value info();
}
