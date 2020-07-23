package org.cafienne.cmmn.instance.casefile;

import org.cafienne.cmmn.instance.TransitionPublisher;
import org.cafienne.cmmn.instance.sentry.CaseFileItemOnPart;
import org.cafienne.cmmn.instance.sentry.StandardEvent;

import java.util.ArrayList;
import java.util.List;

public class BootstrapCaseFileTransitionPublisher extends TransitionPublisher<CaseFileItem, CaseFileItemOnPart> {
    private final List<StandardEvent> bootstrapEvents = new ArrayList();

    BootstrapCaseFileTransitionPublisher(CaseFileItem item) {
        super(item);
        addDebugInfo(() -> "Creating delayed publisher for item " + item);
    }

    @Override
    public void addEvent(StandardEvent event) {
        addDebugInfo(() -> "Adding delayed event " + event.getTransition() + " to myself");
        bootstrapEvents.add(event);
    }

    @Override
    public void releaseBootstrapEvents() {
        addDebugInfo(() -> "Releasing "+ bootstrapEvents.size()+" events from case file publisher of item " + item);
        bootstrapEvents.forEach(event -> super.informEntryCriteria(event));
        bootstrapEvents.forEach(event -> super.informExitCriteria(event));
    }

    @Override
    public void informEntryCriteria(StandardEvent transition) {
        // Cannot yet inform entry criteria, as they are not yet created in the case plan
        addDebugInfo(() -> "  Delay informing entry criteria for event " + transition.getTransition() +" in item " + item.getPath());
    }

    @Override
    public void informExitCriteria(StandardEvent transition) {
        // Cannot yet inform exit criteria, as they are not yet created in the case plan
        addDebugInfo(() -> "  Delay informing exit criteria for event " + transition.getTransition() + " in item " + item.getPath());
    }
}
