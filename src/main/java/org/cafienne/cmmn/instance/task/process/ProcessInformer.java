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

import org.cafienne.cmmn.definition.ProcessTaskDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.ValueMap;

abstract class ProcessInformer {
    static ProcessInformer getInstance(ProcessTask task, ProcessTaskDefinition definition) {
        if (definition.getImplementationDefinition().getImplementation().isInline()) {
            return new ProcessTaskInlineInformer(task);
        } else {
            return new ProcessTaskActorInformer(task);
        }
    }

    protected final ProcessTask task;

    protected ProcessInformer(ProcessTask task) {
        this.task = task;
    }

    protected Case getCaseInstance() {
        return task.getCaseInstance();
    }

    abstract protected void startImplementation(ValueMap inputParameters);

    abstract protected void terminateImplementation();

    abstract protected void suspendImplementation();

    abstract protected void resumeImplementation();

    abstract protected void reactivateImplementation(ValueMap inputParameters);

    abstract protected void migrateDefinition(ProcessTaskDefinition newDefinition);
}
