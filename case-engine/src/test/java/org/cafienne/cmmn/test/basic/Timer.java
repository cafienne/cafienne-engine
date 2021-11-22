/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.TestUser;
import org.junit.Test;

public class Timer {
    
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/timer.xml");
    private final TestUser testUser = TestScript.getTestUser("Anonymous");
    
    @Test
    public void testTimer() {
        String caseInstanceId = "Timer";
        TestScript testCase = new TestScript(caseInstanceId);

        // Case contains a timer that runs after 3 seconds; it then starts a task.
        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertPlanItem("PeriodWaiter").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Waiting 1 second should not have changed anything; timer is still running
        testCase.addStep(testCase.createPingCommand(testUser, caseInstanceId, 1000), casePlan -> {
            casePlan.assertPlanItem("PeriodWaiter").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });
    
        // Waiting 5 seconds should have triggered the timer and the task should now be active
        testCase.addStep(testCase.createPingCommand(testUser, caseInstanceId, 5000), casePlan -> {
            casePlan.assertPlanItem("PeriodWaiter").assertLastTransition(Transition.Occur, State.Completed, State.Available);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Start, State.Active, State.Available);
        });
    
        testCase.runTest();
    }
}
