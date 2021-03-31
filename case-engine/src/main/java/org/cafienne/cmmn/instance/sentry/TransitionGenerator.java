package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.TransitionPublisher;

public interface TransitionGenerator<T extends CMMNElement<?>, E extends StandardEvent> {
    void updateState(E event, TransitionPublisher<TransitionGenerator<T,E>,?> publisher);

    String getDescription();

    Case getCaseInstance();
}
