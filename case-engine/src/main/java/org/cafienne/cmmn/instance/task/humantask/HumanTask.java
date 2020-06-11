/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.task.humantask;

import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.definition.task.validation.TaskOutputValidatorDefinition;
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.validation.TaskOutputValidator;
import org.cafienne.cmmn.instance.task.validation.ValidationResponse;
import org.cafienne.cmmn.instance.team.Member;
import org.cafienne.humantask.akka.event.*;
import org.cafienne.humantask.instance.WorkflowTask;
import org.w3c.dom.Element;

import java.util.Collection;

public class HumanTask extends Task<HumanTaskDefinition> {

    private final WorkflowTask workflow;
    private final TaskOutputValidator validator;

    public HumanTask(String id, int index, ItemDefinition itemDefinition, HumanTaskDefinition definition, Stage stage) {
        super(id, index, itemDefinition, definition, stage);

        // Create an instance of the output validator if we have one
        TaskOutputValidatorDefinition outputValidator = getDefinition().getTaskOutputValidator();
        if (outputValidator != null) {
            validator = outputValidator.createInstance(this);
        } else {
            validator = null;
        }
        this.workflow = getDefinition().getImplementationDefinition().createInstance(this);
    }

    public <T extends HumanTaskEvent> T addEvent(T event) {
        return getCaseInstance().addEvent(event);
    }

    /**
     * Returns the state tracking implementation of the task
     *
     * @return
     */
    public WorkflowTask getImplementation() {
        return workflow;
    }

    @Override
    public ValidationResponse validateOutput(ValueMap potentialRawOutput) {
        if (validator != null) {
            ValidationResponse response = validator.validate(potentialRawOutput);
            if (!response.isValid()) {
                addDebugInfo(() -> "Ouput validation for task " + getName() + "[" + getId() + "] failed with ", response.getContent());
            } else {
                addDebugInfo(() -> "Ouput validation for task " + getName() + "[" + getId() + "] succeeded with ", response.getContent());
            }
            return response;
        } else {
            // Using default validation (checks mandatory parameters to have a value, should also check against case file definition)
            return super.validateOutput(potentialRawOutput);
        }
    }

    @Override
    protected void createInstance() {
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        getCaseInstance().addEvent(new HumanTaskCreated(this));
        getImplementation().beginLifeCycle();
        getCaseInstance().addEvent(new HumanTaskInputSaved(this, getMappedInputParameters()));

    }

    @Override
    protected void terminateInstance() {
        if (getHistoryState() == State.Available) {
            addDebugInfo(() -> "Terminating human task '" + getName() + "' without it being started; no need to inform the task actor");
        } else {
            addEvent(new HumanTaskTerminated(this));
        }
    }

    @Override
    protected void suspendInstance() {
        addEvent(new HumanTaskSuspended(this));
    }

    @Override
    protected void resumeInstance() {
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
    protected boolean isTransitionAllowed(Transition transition) {
        if (transition != Transition.Complete) {
            return true;
        }

        // Now check if the user has the performer role.
        if (!currentUserIsAuthorized()) {
            throw new TransitionDeniedException("You do not have the permission to perform the task " + getName());
        }

        return true;
    }

    /**
     * Checks whether the current user has the proper roles to perform this task
     *
     * @return
     */
    public boolean currentUserIsAuthorized() {
        CaseRoleDefinition performer = getPerformer();
        if (performer == null) { // Only do authorization if the performer role is set
            return true;
        }

        // Now check if the user has the performer role.
        return getCaseInstance().getCurrentTeamMember().getRoles().contains(getPerformer());
    }

    @Override
    protected void dumpImplementationToXML(Element planItemXML) {
        super.dumpImplementationToXML(planItemXML);
//        workflow.dumpMemoryStateToXML(planItemXML);
        CaseRoleDefinition performer = getPerformer();
        if (performer != null) {
            String roleName = performer.getName();
            Element roleElement = planItemXML.getOwnerDocument().createElement("Role");
            planItemXML.appendChild(roleElement);
            roleElement.setAttribute("name", roleName);
        }
    }

//	public void goComplete(ValueMap rawOutput) {
//        ValidationResponse validate = this.validateOutput(rawOutput);
//        if (validate instanceof ValidationError) {
//            throw new InvalidCommandException("Output for task "+this.getName()+" could not be validated due to an error", validate.getException());
//        } else {
//            if (! validate.getContent().getValue().isEmpty()) {
//                throw new InvalidCommandException("Output for task "+this.getName()+" is invalid\n" + validate.getContent());
//            }
//        }
//		super.goComplete(workflow.saveOutput(rawOutput));
//		workflow.goComplete();
//	}
}
