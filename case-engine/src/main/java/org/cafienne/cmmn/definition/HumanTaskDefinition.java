/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.task.WorkflowTaskDefinition;
import org.cafienne.cmmn.definition.task.validation.TaskOutputValidatorDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.w3c.dom.Element;

public class HumanTaskDefinition extends TaskDefinition<WorkflowTaskDefinition> {
    private final PlanningTableDefinition planningTable;
    private final String performerRef;
    private CaseRoleDefinition performer;
    private final WorkflowTaskDefinition wtd;
    private final String taskOutputValidatorRef;
    private TaskOutputValidatorDefinition taskOutputValidator;

    public HumanTaskDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        planningTable = parse("planningTable", PlanningTableDefinition.class, false);
        performerRef = parseAttribute("performerRef", false, "");

        // CMMN 1.0 spec page 37:
        // A HumanTask that is non-blocking (isBlocking set to "false") MUST NOT have a PlanningTable.
        if (!isBlocking()) {
            if (getPlanningTable() != null) {
                getCaseDefinition().addDefinitionError("HumanTask " + getName() + " is non blocking and therefore may not have a planning table");
            }
        }
        
        wtd = parseWorkflowTaskDefinition();
        taskOutputValidatorRef = wtd.getElement().getAttribute("validatorRef");
    }

    private WorkflowTaskDefinition parseWorkflowTaskDefinition() {
        WorkflowTaskDefinition def = getExtension("implementation", WorkflowTaskDefinition.class, false);
        if (def == null) {
            // If we cannot find the extension, we'll create an empty one.
            Element customTag = getElement().getOwnerDocument().createElementNS(NAMESPACE_URI, "implementation");
            getElement().appendChild(customTag);
            def = new WorkflowTaskDefinition(customTag, getDefinition(), this);
        }
        return def;
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    /**
     * Returns the process that can validate output for this task
     * @return
     */
    public TaskOutputValidatorDefinition getTaskOutputValidator() {
        return taskOutputValidator;
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        if (!performerRef.isEmpty()) {
            performer = getCaseDefinition().resolveRoleReference(performerRef, "Human Task " + this);
        }
        if (!taskOutputValidatorRef.isEmpty()) {
            ProcessDefinition pd = getCaseDefinition().getDefinitionsDocument().getProcessDefinition(this.taskOutputValidatorRef);
            if (pd == null) {
                getDefinition().addReferenceError("The task output validator in human task '" + this.getName() + "' refers to a process named " + taskOutputValidatorRef + ", but that definition is not found");
                return; // Avoid further checking on this element
            }
            this.taskOutputValidator = new TaskOutputValidatorDefinition(pd);
        }
    }

    @Override
    public HumanTask createInstance(String id, int index, ItemDefinition itemDefinition, Stage stage, Case caseInstance) {
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
        return wtd;
    }
}
