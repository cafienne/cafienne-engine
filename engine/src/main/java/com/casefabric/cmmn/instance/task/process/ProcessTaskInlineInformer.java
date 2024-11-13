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

package com.casefabric.cmmn.instance.task.process;

import com.casefabric.cmmn.definition.ProcessTaskDefinition;
import com.casefabric.json.ValueMap;
import com.casefabric.processtask.implementation.InlineSubProcess;

class ProcessTaskInlineInformer extends ProcessInformer {

    private final InlineSubProcess<?> implementation;

    public ProcessTaskInlineInformer(ProcessTask task) {
        super(task);
        implementation = task.getDefinition().getImplementationDefinition().getInlineImplementation().createInstance(task);
    }

    @Override
    protected void terminateImplementation() {
        implementation.terminate();
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        implementation.start();
    }

    @Override
    protected void suspendImplementation() {
        implementation.suspend();
    }

    @Override
    protected void resumeImplementation() {
        implementation.resume();
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        implementation.reactivate();
    }

    @Override
    protected void migrateDefinition(ProcessTaskDefinition newDefinition) {
        implementation.migrateDefinition(newDefinition.getImplementationDefinition().getInlineImplementation());
    }
}
