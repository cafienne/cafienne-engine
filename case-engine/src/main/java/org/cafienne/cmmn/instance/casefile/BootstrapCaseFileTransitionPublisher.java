package org.cafienne.cmmn.instance.casefile;

import org.cafienne.cmmn.actorapi.event.file.CaseFileEvent;

import java.util.ArrayList;
import java.util.List;

public class BootstrapCaseFileTransitionPublisher extends CaseFileTransitionPublisher {
    private final List<CaseFileEvent> bootstrapEvents = new ArrayList<>();

    BootstrapCaseFileTransitionPublisher(CaseFileItem item) {
        super(item);
        addDebugInfo(() -> "Creating delayed publisher for item " + item);
    }

    @Override
    public void addEvent(CaseFileEvent event) {
        addDebugInfo(() -> "Adding delayed event " + event.getTransition() + " to myself");
        bootstrapEvents.add(event);
        super.updateItemState(event);
    }

    @Override
    public void releaseBootstrapEvents() {
        addDebugInfo(() -> "Releasing " + bootstrapEvents.size() + " events from case file publisher of item " + item);
        bootstrapEvents.forEach(super::informSentryNetwork);
    }
}
