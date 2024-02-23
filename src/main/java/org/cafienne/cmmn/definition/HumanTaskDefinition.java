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

import org.cafienne.cmmn.definition.extension.workflow.WorkflowTaskDefinition;
import org.cafienne.cmmn.definition.extension.workflow.validation.TaskOutputValidatorDefinition;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItemType;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.w3c.dom.Element;

public class HumanTaskDefinition extends TaskDefinition<WorkflowTaskDefinition> {
    private final PlanningTableDefinition planningTable;
    private final String performerRef;
    private CaseRoleDefinition performer;
    private final WorkflowTaskDefinition workflowDefinition;
    private final String taskOutputValidatorRef;
    private TaskOutputValidatorDefinition taskOutputValidator;

    public HumanTaskDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        planningTable = parse("planningTable", PlanningTableDefinition.class, false);
        performerRef = parseAttribute("performerRef", false, "");

        // CMMN 1.0 spec page 37:
        // A HumanTask that is non-blocking (isBlocking set to "false") MUST NOT have a PlanningTable.
        if (!isBlocking()) {
            if (getPlanningTable() != null) {
                getCaseDefinition().addDefinitionError("HumanTask " + getName() + " is non blocking and therefore may not have a planning table");
            }
        }

        workflowDefinition = parseWorkflowTaskDefinition();
        taskOutputValidatorRef = workflowDefinition.getElement().getAttribute("validatorRef");
    }

    @Override
    public PlanItemType getItemType() {
        return PlanItemType.HumanTask;
    }

    private WorkflowTaskDefinition parseWorkflowTaskDefinition() {
        WorkflowTaskDefinition def = parseExtension(CAFIENNE_IMPLEMENTATION, WorkflowTaskDefinition.class);
        // If we cannot find the extension, we'll create an empty one.
        return def != null ? def : WorkflowTaskDefinition.createEmptyDefinition(this);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    /**
     * Returns the process that can validate output for this task
     *
     * @return
     */
    public TaskOutputValidatorDefinition getTaskOutputValidator() {
        return taskOutputValidator;
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        if (!performerRef.isEmpty()) {
            performer = getCaseDefinition().getCaseTeamModel().resolveRoleReference(performerRef, "Human Task " + this);
        }
        if (!taskOutputValidatorRef.isEmpty()) {
            ProcessDefinition pd = getCaseDefinition().getDefinitionsDocument().getProcessDefinition(this.taskOutputValidatorRef);
            if (pd == null) {
                getModelDefinition().addReferenceError("The task output validator in human task '" + this.getName() + "' refers to a process named " + taskOutputValidatorRef + ", but that definition is not found");
                return; // Avoid further checking on this element
            }
            this.taskOutputValidator = new TaskOutputValidatorDefinition(pd);
        }
    }

    @Override
    public HumanTask createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        return new HumanTask(id, index, itemDefinition, this, stage);
    }

    public PlanningTableDefinition getPlanningTable() {
        return planningTable;
    }

    public CaseRoleDefinition getPerformer() {
        return performer;
    }

    @Override
    public WorkflowTaskDefinition getImplementationDefinition() {
        return workflowDefinition;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameHumanTask);
    }

    public boolean sameHumanTask(HumanTaskDefinition other) {
        return sameTask(other)
                && same(planningTable, other.planningTable)
                && same(performer, other.performer)
                && same(workflowDefinition, other.workflowDefinition)
                && same(taskOutputValidator, other.taskOutputValidator);
    }
}
