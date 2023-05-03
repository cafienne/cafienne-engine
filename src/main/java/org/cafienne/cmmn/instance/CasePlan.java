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

package org.cafienne.cmmn.instance;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.command.plan.task.CompleteTask;
import org.cafienne.cmmn.actorapi.command.plan.task.FailTask;
import org.cafienne.cmmn.actorapi.event.CaseOutputFilled;
import org.cafienne.cmmn.definition.CasePlanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasePlan extends Stage<CasePlanDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(CasePlan.class);

    public CasePlan(String id, CasePlanDefinition definition, Case caseInstance) {
        super(id, 0, definition, definition, null, caseInstance, StateMachine.CasePlan);
    }

    @Override
    protected void suspendInstance() {
        super.suspendInstance();
        informParent(Transition.Suspend);
    }

    @Override
    protected void completeInstance() {
        super.completeInstance();
        addEvent(new CaseOutputFilled(getCaseInstance(), getCaseInstance().getOutputParameters()));
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
        CaseUserIdentity user = getCaseInstance().getCurrentUser();
        informParent(() -> new MakePlanItemTransition(user, parentCaseId, parentCaseTaskId, transition));
    }

    private void informParent(CommandCreator createIfParent) {
        String parentCaseId = getCaseInstance().getParentCaseId(); // Id of our parent
        if (parentCaseId.isEmpty()) {
            // No need to inform about our transitions.
            return;
        }
        CaseCommand command = createIfParent.createCommand();
        getCaseInstance().informParent(command, failure ->
            // TTD: this needs better handling

            // Wow, now what? CaseTask did not accept our information, but why??
            //  And... should we handle this by e.g. going to Fault state? Or what? Can we make this inconsistency clear somehow other than through the log file? Generate a special event or so?
            logger.error("Parent case " + parentCaseId + " did not accept our request " + command + " and responded with a failure\n" + failure));
    }

    @Override
    public void migrateDefinition(CasePlanDefinition newDefinition, boolean skipLogic) {
        addDebugInfo(() -> "\nMigrating Case Plan");
        migrateItemDefinition(newDefinition, newDefinition, skipLogic);
        addDebugInfo(() -> "Completed Case Plan migration\n");
    }

    @FunctionalInterface
    interface CommandCreator {
        CaseCommand createCommand();
    }
}
