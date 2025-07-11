/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.engine.cmmn.test.basic;

import org.cafienne.engine.cmmn.actorapi.command.StartCase;
import org.cafienne.engine.cmmn.definition.CaseDefinition;
import org.cafienne.engine.cmmn.instance.State;
import org.cafienne.engine.cmmn.instance.Transition;
import org.cafienne.engine.cmmn.test.TestScript;
import org.junit.Test;

import static org.cafienne.engine.cmmn.test.TestScript.*;

public class TestTimer {
    
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/timer.xml");

    @Test
    public void testTimer() {
        String caseInstanceId = "Timer";
        TestScript testCase = new TestScript(caseInstanceId);

        // Case contains a timer that runs after 3 seconds; it then starts a task.
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertPlanItem("PeriodWaiter").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Waiting 1 second should not have changed anything; timer is still running
        testCase.addStep(createPingCommand(testUser, caseInstanceId, 1000), casePlan -> {
            casePlan.assertPlanItem("PeriodWaiter").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });
    
        // Waiting 5 seconds should have triggered the timer and the task should now be active
        testCase.addStep(createPingCommand(testUser, caseInstanceId, 5000), casePlan -> {
            casePlan.assertPlanItem("PeriodWaiter").assertLastTransition(Transition.Occur, State.Completed, State.Available);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Start, State.Active, State.Available);
        });
    
        testCase.runTest();
    }
}
