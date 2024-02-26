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

package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItemType;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.task.process.ProcessTask;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.w3c.dom.Element;

public class ProcessTaskDefinition extends TaskDefinition<ProcessDefinition> {
    private final String processRef;
    private ProcessDefinition processDefinition;

    public ProcessTaskDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.processRef = parseAttribute("processRef", true);
    }

    @Override
    public PlanItemType getItemType() {
        return PlanItemType.ProcessTask;
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        this.processDefinition = getCaseDefinition().getDefinitionsDocument().getProcessDefinition(this.processRef);
        if (this.processDefinition == null) {
            getModelDefinition().addReferenceError("The process task '" + this.getName() + "' refers to a process named " + processRef + ", but that definition is not found");
            return; // Avoid further checking on this element
        }
    }

    @Override
    public ProcessTask createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        return new ProcessTask(id, index, itemDefinition, this, stage);
    }

    @Override
    public ProcessDefinition getImplementationDefinition() {
        return processDefinition;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameProcessTask);
    }

    public boolean sameProcessTask(ProcessTaskDefinition other) {
        return sameTask(other)
                && processDefinition.sameProcessDefinition(other.processDefinition);
    }
}
