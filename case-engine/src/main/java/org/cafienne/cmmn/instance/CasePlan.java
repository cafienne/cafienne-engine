/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.definition.CasePlanDefinition;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.task.CompleteTask;
import org.cafienne.cmmn.akka.command.task.FailTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasePlan extends Stage<CasePlanDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(CasePlan.class);

    public CasePlan(String id, CasePlanDefinition definition, Case caseInstance) {
        super(id, 0, definition, definition, null, caseInstance, StateMachine.CasePlan);
        // We are the case plan, so make sure we connect the exit criteria
        //  can be done only here, because collection is created in constructor of stage, and hence
        //  not available in constructor of PlanItem
        definition.getExitCriteria().forEach(c -> getExitCriteria().add(getCriterion(c, this)));
        caseInstance.setCasePlan(this);
    }

    @Override
    protected void suspendInstance() {
        super.suspendInstance();
        informParent(Transition.Suspend);
    }

    @Override
    protected void completeInstance() {
        super.completeInstance();
        informParent(new CompleteTask(getCaseInstance(), getCaseInstance().getOutputParameters()));
    }

    @Override
    protected void terminateInstance() {
        super.terminateInstance();
        informParent(Transition.Terminate);
    }

    @Override
    protected void failInstance() {
        super.failInstance();
        informParent(new FailTask(getCaseInstance(), getCaseInstance().getOutputParameters()));
    }

    private void informParent(Transition transition) {
        String parentCaseTaskId = getCaseInstance().getId(); // Our Id within our parent
        String parentCaseTaskName = ""; // We do not know the name of our planitem in the parent, but it is also not required.
        Case qase = getCaseInstance();
        CaseCommand command = new MakePlanItemTransition(qase.getCurrentUser(), qase.getParentCaseId(), parentCaseTaskId, transition, parentCaseTaskName);
        informParent(command);
    }

    private void informParent(CaseCommand command) {
        String parentCaseId = getCaseInstance().getParentCaseId(); // Id of our parent
        if (parentCaseId.isEmpty()) {
            // No need to inform about our transitions.
            return;
        }

        getCaseInstance().askCase(command, failure ->
            // TTD: this needs better handling

            // Wow, now what? CaseTask did not accept our information, but why??
            //  And... should we handle this by e.g. going to Fault state? Or what? Can we make this inconsistency clear somehow other than through the log file? Generate a special event or so?
            logger.error("Parent case " + parentCaseId + " did not accept our request " + command + " and responded with a failure\n" + failure));
    }
}
