/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.PlanItemDefinitionDefinition;

public abstract class TaskStage<T extends PlanItemDefinitionDefinition> extends PlanItem<T> {
    protected TaskStage(String id, int index, ItemDefinition itemDefinition, T definition, Case caseInstance, Stage<?> parent, StateMachine stateMachine) {
        super(id, index, itemDefinition, definition, caseInstance, parent, stateMachine);
    }

    protected TaskStage(String id, int index, ItemDefinition itemDefinition, T definition, Case caseInstance, Stage<?> parent) {
        this(id, index, itemDefinition, definition, caseInstance, parent, StateMachine.TaskStage);
    }

    /**
     * Entry transition comes out of the evalutation of ManualActivationRule.
     * This is currently not stored as an event in the journal. You may well consider that a bug.
     * Instead of solving that, we decided to for now just evaluate the rule once per runtime instantiation of the item and only when requested.
     */
    private Transition entryTransition = null;

    @Override
    protected Transition getEntryTransition() {
        if (entryTransition == null) {
            // Now evaluate manual activation and trigger the associated transition on the plan item (yeah yeah yeah should be stored as an event, but not now)
            entryTransition = evaluateManualActivationRule();
        }
        return entryTransition;
    }
}
