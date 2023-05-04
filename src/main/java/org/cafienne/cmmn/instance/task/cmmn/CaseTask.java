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

package org.cafienne.cmmn.instance.task.cmmn;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.migration.MigrateDefinition;
import org.cafienne.cmmn.actorapi.command.plan.MakeCaseTransition;
import org.cafienne.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.CaseTaskDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseTask extends Task<CaseTaskDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(CaseTask.class);
    private final String subCaseId;
    private final Case mainCase;

    public CaseTask(String id, int index, ItemDefinition itemDefinition, CaseTaskDefinition definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage);
        subCaseId = getId(); // Our planitem id will also be the id of the subcase.
        mainCase = getCaseInstance();
    }

    @Override
    protected void terminateInstance() {
        if (getHistoryState() == State.Available) {
            addDebugInfo(() -> "Terminating human task '" + getName() + "' without it being started; no need to inform the task actor");
        } else {
            tell(Transition.Terminate);
        }
    }

    @Override
    protected void suspendInstance() {
        tell(Transition.Suspend);
    }

    @Override
    protected void resumeInstance() {
        tell(Transition.Reactivate);
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        // TODO: this must be done with a new command, to reactive a case.
        //  Case Reactivation should reset the input parameters as well...
        //  NOT SURE what this actually means in practice...
        //  For now, we'll just try to start. This will probably run into failures as well. But that's ok for now.
        startImplementation(inputParameters);
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        // Only instantiate the subcase if our plan item has been started, not when it is being resumed
        CaseDefinition subCaseDefinition = getDefinition().getImplementationDefinition();
        String parentCaseId = mainCase.getId();
        String rootCaseId = mainCase.getRootCaseId();
        ValueMap caseInputParameters = getMappedInputParameters();
        CaseTeam caseTeam = mainCase.getCaseTeam().createSubCaseTeam(subCaseDefinition);

        StartCase startCaseCommand = new StartCase(getCaseInstance().getTenant(), getCaseInstance().getCurrentUser(), subCaseId, subCaseDefinition, caseInputParameters, caseTeam, getCaseInstance().debugMode(), parentCaseId, rootCaseId);
        startTaskImplementation(startCaseCommand);
    }

    /**
     * Informs the sub case Actor of the message. Note: this is currently an improper implementation, since the parent case only sends the command
     * (i.e., it is asynchronous). CMMN expects this to be synchronous.
     *
     * @param transition
     */
    private void tell(Transition transition) {
        tellTaskImplementation(new MakeCaseTransition(getCaseInstance().getCurrentUser(), subCaseId, transition));
    }

    @Override
    public void migrateItemDefinition(ItemDefinition newItemDefinition, CaseTaskDefinition newDefinition, boolean skipLogic) {
        super.migrateItemDefinition(newItemDefinition, newDefinition, skipLogic);
        if (skipLogic) return;

        giveNewDefinition(new MigrateDefinition(getCaseInstance().getCurrentUser(), getId(), newDefinition.getImplementationDefinition()));
    }

    @Override
    protected void lostDefinition() {
        addDebugInfo(() -> "Dropping CaseTasks through migration is not possible. Task[" + getPath() + "] remains in the case with current state '" + getState() + "'");
    }
}
