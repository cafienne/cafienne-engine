package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;

import java.util.ArrayList;
import java.util.List;

public class TransitionPublisher<E extends StandardEvent<?,?>, I extends TransitionGenerator<E>, P extends OnPart<?, E, I>> {
    protected final I item;
    private final List<E> transitions = new ArrayList<>();
    private final List<P> connectedEntryCriteria = new ArrayList<>();
    private final List<P> connectedExitCriteria = new ArrayList<>();

    public TransitionPublisher(I item) {
        this.item = item;
    }

    /**
     * Special constructor to be invoked from Bootstrap publisher.
     * It inherits the already bound entry and exit criteria.
     * @param bootstrapPublisher
     */
    protected TransitionPublisher(TransitionPublisher<E, I, P> bootstrapPublisher) {
        // Release bootstrap publishers' events. This potentially releases some of the entry and exit criteria
        bootstrapPublisher.releaseBootstrapEvents();
        // After release of bootstrap events, we start listening ourselves, with updated set of entry and exit criteria
        this.item = bootstrapPublisher.item;
        this.connectedEntryCriteria.addAll(bootstrapPublisher.connectedEntryCriteria);
        this.connectedExitCriteria.addAll(bootstrapPublisher.connectedExitCriteria);
    }

    public void addEvent(E event) {
        transitions.add(0, event);
        updateItemState(event);
        informSentryNetwork(event);
    }

    protected void updateItemState(E event) {
        item.updateStandardEvent(event);
    }

    protected void informSentryNetwork(E event) {
        addDebugInfo(() -> "Informing sentry network about " + event.getTransition() +" in " + item.getDescription());
        item.getCaseInstance().getSentryNetwork().handleTransition(event, this);
    }

    /**
     * Generic hook for releasing bootstrap events from case file.
     */
    public void releaseBootstrapEvents() {
    }

    public void connectOnPart(P onPart) {
        if (onPart.getCriterion().isEntryCriterion()) {
            insertOnPart(onPart, connectedEntryCriteria);
        } else {
            insertOnPart(onPart, connectedExitCriteria);
        }

        // We only inform the first item of a transition that happened "in the past".
        //  Repetition items only need to react to new events. This check avoids potential endless recursion.
        if (onPart.getCriterion().getTarget().getIndex() == 0 && !transitions.isEmpty()) {
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
        Stage<?> onPartStage = onPart.getCriterion().getStage();
        int i = 0;
        // Iterate the list until we encounter an onPart that does not contain the new criterion.
        while (i < list.size() && list.get(i).getCriterion().getStage().contains(onPartStage)) {
            i++;
        }
        list.add(i, onPart);
    }

    public void informEntryCriteria(E transition) {
        if (! connectedEntryCriteria.isEmpty()) {
            addDebugInfo(() -> "Informing " + connectedEntryCriteria.size() +" entry criteria that listen to item " + item);
        }
        // Then inform the activating sentries
        new ArrayList<>(connectedEntryCriteria).forEach(onPart -> onPart.inform(item, transition));
    }

    public void informExitCriteria(E transition) {
        if (! connectedExitCriteria.isEmpty()) {
            addDebugInfo(() -> "Informing " + connectedExitCriteria.size() +" exit criteria that listen to item " + item);
        }
        // Then inform the activating sentries
        new ArrayList<>(connectedExitCriteria).forEach(onPart -> onPart.inform(item, transition));
    }

    public void releaseOnPart(P onPart) {
        if (onPart.getCriterion().isEntryCriterion()) {
            connectedEntryCriteria.remove(onPart);
        } else {
            connectedExitCriteria.remove(onPart);
        }
    }

    protected void addDebugInfo(DebugStringAppender appender) {
        item.getCaseInstance().addDebugInfo(appender);
    }
}
