package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.akka.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.instance.sentry.OnPart;
import org.cafienne.cmmn.instance.sentry.PlanItemOnPart;
import org.cafienne.cmmn.instance.sentry.StandardEvent;

import java.util.ArrayList;
import java.util.List;

class TransitionPublisher<I extends CMMNElement<?>, P extends OnPart<?,I>> {
    private final I item;
    private final List<StandardEvent> transitions = new ArrayList<>();

    TransitionPublisher(I item) {
        this.item = item;
    }

    void addEvent(StandardEvent event) {
        transitions.add(0, event);
    }

    /**
     * Outgoing criteria (i.e., for plan items interested in our transitions)
     */
    private final List<P> connectedEntryCriteria = new ArrayList<>();
    private final List<P> connectedExitCriteria = new ArrayList<>();

    public void connectOnPart(P onPart) {
        if (onPart.getCriterion().isEntryCriterion()) {
            insertOnPart(onPart, connectedEntryCriteria);
        } else {
            insertOnPart(onPart, connectedExitCriteria);
        }
        if (! transitions.isEmpty()) {
            onPart.inform(item, transitions.get(0));
        }
    }

    /**
     * Inserts the onPart in the right location of the plan item hierarchy
     *
     * @param onPart
     * @param list
     */
    private void insertOnPart(P onPart, List<P> list) {
        if (list.contains(onPart)) {
            return; // do not connect more than once
        }
        Stage onPartStage = onPart.getCriterion().getStage();
        int i = 0;
        // Iterate the list until we encounter an onPart that does not contain the new criterion.
        while (i < list.size() && list.get(i).getCriterion().getStage().contains(onPartStage)) {
            i++;
        }
        list.add(i, onPart);
    }

    void informEntryCriteria(StandardEvent transition) {
        // Then inform the activating sentries
        new ArrayList<>(connectedEntryCriteria).forEach(onPart -> onPart.inform(item, transition));
    }

    void informExitCriteria(StandardEvent transition) {
        // Then inform the activating sentries
        new ArrayList<>(connectedExitCriteria).forEach(onPart -> onPart.inform(item, transition));
    }
}
