/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.sentry;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.TestUser;
import org.junit.Test;

import static org.cafienne.cmmn.test.TestScript.*;

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
