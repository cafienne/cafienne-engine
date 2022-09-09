package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Path;

public interface TransitionGenerator<E extends StandardEvent<?,?>> {
    void updateStandardEvent(E event);

    default String getDescription(){
        return getClass().getSimpleName() + "[" + getPath() + "]";
    }

    Path getPath();

    Case getCaseInstance();

    TransitionPublisher<E,?,?> getPublisher();
}
