/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.definition.EventListenerDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;

public abstract class EventListener<D extends EventListenerDefinition> extends PlanItem<D> {
    protected EventListener(String id, int index, ItemDefinition itemDefinition, D definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage, StateMachine.EventMilestone);
    }

    @Override
    void evaluateRepetitionRule(boolean firstEvaluation) {
        // EventListeners have no repetition
    }

    @Override
    void evaluateRequiredRule() {
        // EventListeners are never 'required'
    }

    @Override
    protected Transition getEntryTransition() {
        // EventListeners do not have an entry transition
        return Transition.None;
    }
}
