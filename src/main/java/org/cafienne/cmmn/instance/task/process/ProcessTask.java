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

package org.cafienne.cmmn.instance.task.process;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ProcessTaskDefinition;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.json.ValueMap;

public class ProcessTask extends Task<ProcessTaskDefinition> {
    private final ProcessInformer informer;

    public ProcessTask(String id, int index, ItemDefinition itemDefinition, ProcessTaskDefinition definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage);
        informer = ProcessInformer.getInstance(this, definition);
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        informer.startImplementation(inputParameters);
    }

    @Override
    protected void suspendImplementation() {
        informer.suspendImplementation();
    }

    @Override
    protected void resumeImplementation() {
        informer.resumeImplementation();
    }

    @Override
    protected void terminateImplementation() {
        informer.terminateImplementation();
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        informer.reactivateImplementation(inputParameters);
    }

    @Override
    protected void migrateItemDefinition(ItemDefinition newItemDefinition, ProcessTaskDefinition newDefinition, boolean skipLogic) {
        super.migrateItemDefinition(newItemDefinition, newDefinition, skipLogic);
        if (getPreviousDefinition().getImplementationDefinition().sameProcessDefinition(newDefinition.getImplementationDefinition())) {
            addDebugInfo(() -> "No need to migrate implementation definition of process task " + getId() + " (proposed implementation equals the current implementation)");
        } else {
            informer.migrateDefinition(newDefinition);
        }
    }

    @Override
    protected void lostDefinition() {
        addDebugInfo(() -> "Dropping ProcessTasks through migration is not possible. Task[" + getPath() + "] remains in the case with current state '" + getState() + "'");
    }
}
