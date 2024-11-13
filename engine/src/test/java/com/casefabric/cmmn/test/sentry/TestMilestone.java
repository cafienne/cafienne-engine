/*
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.casefabric.cmmn.test.sentry;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.actorapi.command.team.CaseTeam;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.cmmn.test.TestUser;
import org.junit.Test;

import static com.casefabric.cmmn.test.TestScript.*;

public class TestMilestone {

    private final String testName = "roles";
    private final String caseInstanceId = testName;
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/milestonetransitions.xml");

    @Test
    public void testDoubleMilestoneTransition() {
        TestScript testCase = new TestScript(testName);
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            casePlan.getEvents().printEventList();
            casePlan.assertPlanItems("HumanTask").assertSize(1).assertStates(State.Active);
            System.out.println("It works");
        });

        testCase.runTest();
    }
}
