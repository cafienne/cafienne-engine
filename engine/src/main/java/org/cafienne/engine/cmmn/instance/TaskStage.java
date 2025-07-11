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

package org.cafienne.engine.cmmn.instance;

import org.cafienne.engine.cmmn.definition.ItemDefinition;
import org.cafienne.engine.cmmn.definition.PlanItemDefinitionDefinition;

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
