/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.cmmn.instance.sentry;

import com.casefabric.cmmn.instance.Stage;
import com.casefabric.cmmn.instance.debug.DebugInfoAppender;

import java.util.ArrayList;
import java.util.List;

public class TransitionPublisher<E extends StandardEvent<?, ?>, I extends TransitionGenerator<E>, P extends OnPart<?, E, I>> {
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
        addDebugInfo(() -> "Informing sentry network about " + event.getTransition() + " in " + item.getDescription());
        item.getCaseInstance().getSentryNetwork().handleTransition(event);
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

        if (onPart.getCaseInstance().recoveryFinished()) {
            // We only inform the first item of a transition that happened "in the past".
            //  Repetition items only need to react to new events. This check avoids potential endless recursion.
            if (onPart.getCriterion().getTarget().getIndex() == 0 && !transitions.isEmpty()) {
                onPart.inform(item, transitions.get(0));
            }
        } else {
            // During recovery, we inform all on parts about our most recent event,
            // so that the sentry network is in the right state after recovery.
            transitions.forEach(event -> onPart.inform(item, event));
        }
    }

    /**
     * Inserts the onPart in the right location of the plan item hierarchy
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
        if (!connectedEntryCriteria.isEmpty()) {
            addDebugInfo(() -> "Informing " + connectedEntryCriteria.size() + " entry criteria that listen to item " + item.getDescription());
        }
        // Then inform the activating sentries
        new ArrayList<>(connectedEntryCriteria).forEach(onPart -> onPart.inform(item, transition));
    }

    public void informExitCriteria(E transition) {
        if (!connectedExitCriteria.isEmpty()) {
            addDebugInfo(() -> "Informing " + connectedExitCriteria.size() + " exit criteria that listen to item " + item.getDescription());
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

    protected void addDebugInfo(DebugInfoAppender appender) {
        item.getCaseInstance().addDebugInfo(appender);
    }
}
