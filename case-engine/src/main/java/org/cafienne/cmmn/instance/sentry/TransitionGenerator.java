package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.instance.Case;

public interface TransitionGenerator<E extends StandardEvent<?,?>> {
    void updateStandardEvent(E event);

    String getDescription();

    Case getCaseInstance();
}
