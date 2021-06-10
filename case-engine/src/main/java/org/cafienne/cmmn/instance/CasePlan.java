/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.definition.CasePlanDefinition;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.command.plan.task.CompleteTask;
import org.cafienne.cmmn.actorapi.command.plan.task.FailTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasePlan extends Stage<CasePlanDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(CasePlan.class);

    public CasePlan(String id, CasePlanDefinition definition, Case caseInstance) {
        super(id, 0, definition, definition, null, caseInstance, StateMachine.CasePlan);
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
        informParent(() -> new CompleteTask(getCaseInstance(), getCaseInstance().getOutputParameters()));
    }

    @Override
    protected void terminateInstance() {
        super.terminateInstance();
        informParent(Transition.Terminate);
    }

    @Override
    protected void failInstance() {
        super.failInstance();
        informParent(() -> new FailTask(getCaseInstance(), getCaseInstance().getOutputParameters()));
    }

    private void informParent(Transition transition) {
        String parentCaseTaskId = getCaseInstance().getId(); // Our Id within our parent
        String parentCaseId = getCaseInstance().getParentCaseId(); // Id of our parent
        TenantUser user = getCaseInstance().getCurrentUser();
        informParent(() -> new MakePlanItemTransition(user, parentCaseId, parentCaseTaskId, transition));
    }

    private void informParent(CommandCreator createIfParent) {
        String parentCaseId = getCaseInstance().getParentCaseId(); // Id of our parent
        if (parentCaseId.isEmpty()) {
            // No need to inform about our transitions.
            return;
        }
        CaseCommand command = createIfParent.createCommand();
        getCaseInstance().askCase(command, failure ->
            // TTD: this needs better handling

            // Wow, now what? CaseTask did not accept our information, but why??
            //  And... should we handle this by e.g. going to Fault state? Or what? Can we make this inconsistency clear somehow other than through the log file? Generate a special event or so?
            logger.error("Parent case " + parentCaseId + " did not accept our request " + command + " and responded with a failure\n" + failure));
    }

    @FunctionalInterface
    interface CommandCreator {
        CaseCommand createCommand();
    }
}
