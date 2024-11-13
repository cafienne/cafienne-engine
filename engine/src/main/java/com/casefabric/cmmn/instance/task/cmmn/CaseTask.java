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

package com.casefabric.cmmn.instance.task.cmmn;

import com.casefabric.cmmn.actorapi.command.ReactivateCase;
import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.actorapi.command.migration.MigrateCaseDefinition;
import com.casefabric.cmmn.actorapi.command.plan.MakeCaseTransition;
import com.casefabric.cmmn.actorapi.command.team.CaseTeam;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.definition.CaseTaskDefinition;
import com.casefabric.cmmn.definition.ItemDefinition;
import com.casefabric.cmmn.instance.*;
import com.casefabric.json.ValueMap;
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
    protected void startImplementation(ValueMap inputParameters) {
        // Only instantiate the subcase if our plan item has been started, not when it is being resumed
        CaseDefinition subCaseDefinition = getDefinition().getImplementationDefinition();
        String parentCaseId = mainCase.getId();
        String rootCaseId = mainCase.getRootCaseId();
        CaseTeam caseTeam = mainCase.getCaseTeam().createSubCaseTeam(subCaseDefinition);

        StartCase command = new StartCase(getCaseInstance().getTenant(), getCaseInstance().getCurrentUser(), subCaseId, subCaseDefinition, inputParameters, caseTeam, getCaseInstance().debugMode(), parentCaseId, rootCaseId);
        startTaskImplementation(command);
    }

    @Override
    protected void suspendImplementation() {
        tell(Transition.Suspend);
    }

    @Override
    protected void resumeImplementation() {
        tell(Transition.Reactivate);
    }

    @Override
    protected void terminateImplementation() {
        if (getHistoryState() == State.Available) {
            addDebugInfo(() -> "Terminating human task '" + getName() + "' without it being started; no need to inform the task actor");
        } else {
            tell(Transition.Terminate);
        }
    }

    @Override
    public void goFault(ValueMap rawOutputParameters) {
        // Ensure faults are not accidentally invoking transformations
        super.goFault(null);
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        // Only instantiate the subcase if our plan item has been started, not when it is being resumed
        CaseDefinition subCaseDefinition = getDefinition().getImplementationDefinition();
        String parentCaseId = mainCase.getId();
        String rootCaseId = mainCase.getRootCaseId();
        CaseTeam caseTeam = mainCase.getCaseTeam().createSubCaseTeam(subCaseDefinition);

        ReactivateCase command = new ReactivateCase(getCaseInstance().getTenant(), getCaseInstance().getCurrentUser(), subCaseId, subCaseDefinition, inputParameters, caseTeam, getCaseInstance().debugMode(), parentCaseId, rootCaseId);
        reactivateTaskImplementation(command);
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

        CaseDefinition newImplementation = newDefinition.getImplementationDefinition();
        CaseTeam newSubCaseTeam = mainCase.getCaseTeam().createSubCaseTeam(newImplementation);

        giveNewDefinition(new MigrateCaseDefinition(getCaseInstance().getCurrentUser(), getId(), newImplementation, newSubCaseTeam));
    }

    @Override
    protected void lostDefinition() {
        addDebugInfo(() -> "Dropping CaseTasks through migration is not possible. Task[" + getPath() + "] remains in the case with current state '" + getState() + "'");
    }
}
