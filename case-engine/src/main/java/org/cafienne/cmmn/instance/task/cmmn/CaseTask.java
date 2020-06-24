/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.task.cmmn;

import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.MakeCaseTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.team.CaseTeam;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.CaseTaskDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseTask extends Task<CaseTaskDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(CaseTask.class);
    private final String subCaseId;
    private final Case mainCase;

    public CaseTask(String id, int index, ItemDefinition itemDefinition, CaseTaskDefinition definition, Stage stage) {
        super(id, index, itemDefinition, definition, stage);
        subCaseId = getId(); // Our planitem id will also be the id of the subcase.
        mainCase = getCaseInstance();
    }

    @Override
    protected void createInstance() {
        // Nothing to be done here; when we start, we invoke StartCase, which will also create the instance
    }

    @Override
    protected void terminateInstance() {
        tell(Transition.Complete);
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

        getCaseInstance().askCase(startCaseCommand,
            left -> goFault(new ValueMap("failure", left.toJson())),
            right -> {
                if (!getDefinition().isBlocking()) {
                    makeTransition(Transition.Complete);
                }
            });
    }

    /**
     * Informs the sub case Actor of the message. Note: this is currently an improper implementation, since the parent case only sends the command
     * (i.e., it is asynchronous). CMMN expects this to be synchronous.
     *
     * @param transition
     */
    private void tell(Transition transition) {
        if (getDefinition().isBlocking()) {
            CaseCommand command = new MakeCaseTransition(getCaseInstance().getCurrentUser(), subCaseId, transition);
            getCaseInstance().askCase(command, left ->
                // Is logging an error sufficient? Or should we go Fault?!
                logger.error("Could not make transition " + transition + " on sub case implementation for task " + subCaseId + "\n" + left));
        }
    }
}
