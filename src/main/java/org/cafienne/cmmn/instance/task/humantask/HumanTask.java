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

package org.cafienne.cmmn.instance.task.humantask;

import org.cafienne.cmmn.actorapi.event.CaseAppliedPlatformUpdate;
import org.cafienne.cmmn.definition.HumanTaskDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.PlanningTableDefinition;
import org.cafienne.cmmn.definition.extension.workflow.validation.TaskOutputValidatorDefinition;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.task.validation.TaskOutputValidator;
import org.cafienne.cmmn.instance.task.validation.ValidationResponse;
import org.cafienne.humantask.actorapi.event.HumanTaskResumed;
import org.cafienne.humantask.actorapi.event.HumanTaskSuspended;
import org.cafienne.humantask.actorapi.event.HumanTaskTerminated;
import org.cafienne.humantask.actorapi.event.migration.HumanTaskDropped;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.json.ValueMap;
import org.w3c.dom.Element;

import java.util.Collection;

public class HumanTask extends Task<HumanTaskDefinition> {

    private WorkflowTask workflow;

    public HumanTask(String id, int index, ItemDefinition itemDefinition, HumanTaskDefinition definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage);
    }

    /**
     * Returns the state tracking implementation of the task
     *
     * @return
     */
    public WorkflowTask getImplementation() {
        if (workflow == null) {
            workflow = getDefinition().getImplementationDefinition().createInstance(this);
        }
        return workflow;
    }

    @Override
    public boolean makeTransition(Transition transition) {
        if (transition == Transition.Complete && getState() == State.Active) {
            return workflow.complete(new ValueMap());
        } else {
            return super.makeTransition(transition);
        }
    }

    @Override
    public ValidationResponse validateOutput(ValueMap potentialRawOutput) {
        // Create an instance of the output validator if we have one
        TaskOutputValidatorDefinition outputValidator = getDefinition().getTaskOutputValidator();
        if (outputValidator != null) {
            TaskOutputValidator validator = outputValidator.createInstance(this);
            ValidationResponse response = validator.validate(potentialRawOutput);
            if (!response.isValid()) {
                addDebugInfo(() -> "Output validation for task " + getName() + "[" + getId() + "] failed with ", response.getContent());
            } else {
                addDebugInfo(() -> "Output validation for task " + getName() + "[" + getId() + "] succeeded with ", response.getContent());
            }
            return response;
        } else {
            // Using default validation (checks mandatory parameters to have a value, should also check against case file definition)
            return super.validateOutput(potentialRawOutput);
        }
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        getImplementation().beginLifeCycle();
    }

    @Override
    protected void terminateImplementation() {
        if (getHistoryState() == State.Available) {
            addDebugInfo(() -> "Terminating human task '" + getName() + "' without it being started; no need to inform the task actor");
        } else {
            addEvent(new HumanTaskTerminated(this));
        }
    }

    @Override
    protected void suspendImplementation() {
        if (getHistoryState().isAlive()) {
            addEvent(new HumanTaskSuspended(this));
        }
    }

    @Override
    protected void resumeImplementation() {
        addEvent(new HumanTaskResumed(this));
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        // Can we re-active a task??? Upon failure, yes. Ok. But then how to implement this???
        //  For now, we'll just try to start again.
        startImplementation(inputParameters);
    }

    @Override
    protected boolean hasDiscretionaryItems() {
        PlanningTableDefinition table = getDefinition().getPlanningTable();
        if (table != null) {
            return table.hasItems(this);
        }
        return false;
    }

    @Override
    protected void retrieveDiscretionaryItems(Collection<DiscretionaryItem> items) {
        PlanningTableDefinition table = getDefinition().getPlanningTable();
        if (table != null) {
            addDebugInfo(() -> "Iterating planning table items in " + this);
            table.evaluate(this, items);
        }
    }

    /**
     * Returns the role that is allowed to perform this human task
     *
     * @return
     */
    public CaseRoleDefinition getPerformer() {
        return getDefinition().getPerformer();
    }

    /**
     * Checks whether the current user is allowed to complete the task
     * Completing the task is here interpreted as "performing the task", see spec page 14, section 5.2.2 on Role.
     */
    @Override
    public void validateTransition(Transition transition) {
        super.validateTransition(transition);
        if (transition == Transition.Complete) {
            // Now check if the user has the performer role.
            if (!getCaseInstance().getCurrentTeamMember().hasRole(getPerformer())) {
                throw new TransitionDeniedException("You do not have the permission to perform the task " + getName());
            }
        }
    }

    @Override
    protected void dumpImplementationToXML(Element planItemXML) {
        super.dumpImplementationToXML(planItemXML);
        CaseRoleDefinition performer = getPerformer();
        if (performer != null) {
            String roleName = performer.getName();
            Element roleElement = planItemXML.getOwnerDocument().createElement("Role");
            planItemXML.appendChild(roleElement);
            roleElement.setAttribute("name", roleName);
        }
    }

    public void updateState(CaseAppliedPlatformUpdate event) {
        getImplementation().updateState(event);
    }

    @Override
    public void migrateItemDefinition(ItemDefinition newItemDefinition, HumanTaskDefinition newDefinition, boolean skipLogic) {
        super.migrateItemDefinition(newItemDefinition, newDefinition, skipLogic);
        getImplementation().migrateDefinition(newDefinition.getImplementationDefinition(), skipLogic);
    }

    @Override
    protected void lostDefinition() {
        super.lostDefinition();
        addEvent(new HumanTaskDropped(this));
    }
}
