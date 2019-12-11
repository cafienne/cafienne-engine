/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.MakeCaseTransition;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.PlanItemAssertion;
import org.cafienne.cmmn.test.assertions.TaskAssertion;
import org.junit.Test;

public class EventListener {
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/eventlistener.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testEventListener() {
        // This is a set of basic tests for events with some related sentries.
        String caseInstanceId = "EventListener";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addTestStep(startCase, action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            TaskAssertion item1 = casePlan.assertTask("T1");
            item1.assertLastTransition(Transition.Create, State.Available, State.Null);

            TaskAssertion item2 = casePlan.assertTask("T2");
            item2.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion codeBasedWaiter = casePlan.assertPlanItem("CodeBasedWaiter");
            codeBasedWaiter.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion userEvent = casePlan.assertPlanItem("UserEvent");
            userEvent.assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Having the user event occur should activate T1
        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Occur, "UserEvent"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            TaskAssertion item1 = casePlan.assertTask("T1");
            item1.assertLastTransition(Transition.Start, State.Active, State.Available);

            TaskAssertion item2 = casePlan.assertTask("T2");
            item2.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion codeBasedWaiter = casePlan.assertPlanItem("CodeBasedWaiter");
            codeBasedWaiter.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion userEvent = casePlan.assertPlanItem("UserEvent");
            userEvent.assertLastTransition(Transition.Occur, State.Completed, State.Available);
        });

        // Completing Task1 should just complete it
        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "T1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            TaskAssertion item1 = casePlan.assertTask("T1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);

            TaskAssertion item2 = casePlan.assertTask("T2");
            item2.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion codeBasedWaiter = casePlan.assertPlanItem("CodeBasedWaiter");
            codeBasedWaiter.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion userEvent = casePlan.assertPlanItem("UserEvent");
            userEvent.assertLastTransition(Transition.Occur, State.Completed, State.Available);
        });   
        
        // Completing Task1 again should not change anything
        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "T1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            TaskAssertion item1 = casePlan.assertTask("T1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);

            TaskAssertion item2 = casePlan.assertTask("T2");
            item2.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion codeBasedWaiter = casePlan.assertPlanItem("CodeBasedWaiter");
            codeBasedWaiter.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion userEvent = casePlan.assertPlanItem("UserEvent");
            userEvent.assertLastTransition(Transition.Occur, State.Completed, State.Available);
        });
        

        // Terminating the case should destroy the waiter event and remaining task
        testCase.addTestStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Terminate), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Terminate, State.Terminated, State.Active);

            TaskAssertion item1 = casePlan.assertTask("T1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);

            TaskAssertion item2 = casePlan.assertTask("T2");
            item2.assertLastTransition(Transition.Exit, State.Terminated, State.Available);

            PlanItemAssertion codeBasedWaiter = casePlan.assertPlanItem("CodeBasedWaiter");
            codeBasedWaiter.assertLastTransition(Transition.ParentTerminate, State.Terminated, State.Available);

            PlanItemAssertion userEvent = casePlan.assertPlanItem("UserEvent");
            userEvent.assertLastTransition(Transition.Occur, State.Completed, State.Available);
        });
        
        testCase.runTest();
    }

}
