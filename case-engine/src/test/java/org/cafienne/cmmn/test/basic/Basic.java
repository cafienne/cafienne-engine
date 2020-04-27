/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.*;
import org.junit.Test;

public class Basic {
    @Test
    public void testBasic() {
        // This tests a set of basic plan item types, such as HumanTask, Stage, Milestone and UserEvent
        // and additionally some Sentries.
        // The test just starts the case and then validates the output, no specific actions are done (no transitions are made)
        String caseInstanceId = "Basic";
        TestScript testCase = new TestScript(caseInstanceId);
        TenantUser testUser = TestScript.getTestUser("Anonymous");


        CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/basic.xml");
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            TaskAssertion item1 = casePlan.assertTask("Item1");
            item1.assertLastTransition(Transition.Start, State.Active, State.Available);

            TaskAssertion item2 = casePlan.assertTask("Item2");
            item2.assertLastTransition(Transition.Start, State.Active, State.Available);

            TaskAssertion item3 = casePlan.assertTask("Item3");
            item3.assertLastTransition(Transition.Create, State.Available, State.Null);

            StageAssertion item4 = casePlan.assertStage("Item4");
            item4.assertLastTransition(Transition.Start, State.Active, State.Available);

            PlanItemAssertion item1dot1 = item4.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.Start, State.Active, State.Available);

            PlanItemAssertion item1dot2 = item4.assertPlanItem("Item1.2");
            item1dot2.assertLastTransition(Transition.Start, State.Active, State.Available);
            
            PlanItemAssertion milestone = casePlan.assertPlanItem("Milestone");
            milestone.assertLastTransition(Transition.Create, State.Available, State.Null);

            PlanItemAssertion listener = casePlan.assertPlanItem("Listener");
            listener.assertLastTransition(Transition.Create, State.Available, State.Null);

            PublishedEventsAssertion startCaseEvents = casePlan.getEvents().filter(caseInstanceId);
            TestScript.debugMessage("Start case generated these events:\n" + startCaseEvents.enumerateEventsByType());
            if (startCaseEvents.getEvents().size() != 56) {
                TestScript.debugMessage("Expected these events:\nCaseDefinitionApplied: 1\n" +
                        "TeamMemberAdded: 1\n" +
                        "PlanItemCreated: 9\n" +
                        "PlanItemTransitioned: 14\n" +
                        "RepetitionRuleEvaluated: 7\n" +
                        "RequiredRuleEvaluated: 7\n" +
                        "TaskInputFilled: 4\n" +
                        "HumanTaskCreated: 4\n" +
                        "HumanTaskActivated: 4\n" +
                        "HumanTaskInputSaved: 4\n" +
                        "CaseModified: 1");
            }
            startCaseEvents.assertSize(56);
        });
        
        // Completing Item1 should activate sentries S3 and S3.2
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "Item1"), casePlan -> {
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            TaskAssertion item1 = casePlan.assertTask("Item1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);

            TaskAssertion item2 = casePlan.assertTask("Item2");
            item2.assertLastTransition(Transition.Start, State.Active, State.Available);

            TaskAssertion item3 = casePlan.assertTask("Item3");
            item3.assertLastTransition(Transition.Start, State.Active, State.Available);

            StageAssertion item4 = casePlan.assertStage("Item4");
            item4.assertLastTransition(Transition.Start, State.Active, State.Available);

            PlanItemAssertion item1dot1 = item4.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.Start, State.Active, State.Available);

            PlanItemAssertion item1dot2 = item4.assertPlanItem("Item1.2");
            item1dot2.assertLastTransition(Transition.Start, State.Active, State.Available);
            
            PlanItemAssertion milestone = casePlan.assertPlanItem("Milestone");
            milestone.assertLastTransition(Transition.Occur, State.Completed, State.Available);

            PlanItemAssertion listener = casePlan.assertPlanItem("Listener");
            listener.assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Completing Item2 should also activate exit criterion of stage Item4, which ought to terminate it's children
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "Item2"), casePlan -> {
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            TaskAssertion item1 = casePlan.assertTask("Item1");
            item1.assertLastTransition(Transition.Complete, State.Completed, State.Active);

            TaskAssertion item2 = casePlan.assertTask("Item2");
            item2.assertLastTransition(Transition.Complete, State.Completed, State.Active);

            TaskAssertion item3 = casePlan.assertTask("Item3");
            item3.assertLastTransition(Transition.Start, State.Active, State.Available);

            StageAssertion item4 = casePlan.assertStage("Item4");
            item4.assertLastTransition(Transition.Exit, State.Terminated, State.Active);

            PlanItemAssertion item1dot1 = item4.assertPlanItem("Item1.1");
            item1dot1.assertLastTransition(Transition.Exit, State.Terminated, State.Active);

            PlanItemAssertion item1dot2 = item4.assertPlanItem("Item1.2");
            item1dot2.assertLastTransition(Transition.Exit, State.Terminated, State.Active);
            
            PlanItemAssertion milestone = casePlan.assertPlanItem("Milestone");
            milestone.assertLastTransition(Transition.Occur, State.Completed, State.Available);

            PlanItemAssertion listener = casePlan.assertPlanItem("Listener");
            listener.assertLastTransition(Transition.Create, State.Available, State.Null);
        });
        
        testCase.runTest();

    }
}
