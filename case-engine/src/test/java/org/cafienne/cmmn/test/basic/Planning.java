/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.cmmn.akka.command.*;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.DiscretionaryItemAssertion;
import org.cafienne.cmmn.test.assertions.PlanningTableAssertion;
import org.cafienne.cmmn.test.assertions.StageAssertion;
import org.cafienne.akka.actor.identity.TenantUser;
import org.junit.Test;

public class Planning {
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/planning.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testPlanning() {
        // This is a set of basic tests for events with some related sentries.
        String caseInstanceId = "planning";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addTestStep(startCase, action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            casePlan.assertTask("T1").assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertTask("T2").assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertTask("T3").assertLastTransition(Transition.Create, State.Available, State.Null);

            testCase.insertTestStep(new GetDiscretionaryItems(testUser, caseInstanceId), items -> {
                PlanningTableAssertion pta = new PlanningTableAssertion(items);
                pta.assertItem("Opnieuw T1");
                pta.assertItem("T4").assertType("HumanTask");
            });
        });

        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "T1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            casePlan.assertTask("T1").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T2").assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertTask("T3").assertLastTransition(Transition.Create, State.Available, State.Null);

            testCase.insertTestStep(new GetDiscretionaryItems(testUser, caseInstanceId), items -> {
                PlanningTableAssertion pta = new PlanningTableAssertion(items);
                pta.assertItem("Opnieuw T1");
                pta.assertItem("T4");

                // Now add T4 to the plan
                DiscretionaryItemAssertion t4 = pta.assertItem("T4");
                String t4stageId = t4.getParentId();
                String definitionId = t4.getDefinitionId();
                testCase.insertTestStep(new AddDiscretionaryItem(testUser, caseInstanceId, "T4", definitionId, t4stageId, "t4item"), action2 -> {
                    CaseAssertion casePlan2 = new CaseAssertion(action2);
                    casePlan2.assertLastTransition(Transition.Create, State.Active, State.Null);

                    casePlan2.assertTask("T1").assertLastTransition(Transition.Complete, State.Completed, State.Active);
                    casePlan2.assertTask("T2").assertLastTransition(Transition.Start, State.Active, State.Available);
                    casePlan2.assertTask("T3").assertLastTransition(Transition.Create, State.Available, State.Null);
                    casePlan2.assertTask("T4").assertLastTransition(Transition.Start, State.Active, State.Available);

                    testCase.insertTestStep(new GetDiscretionaryItems(testUser, caseInstanceId), items2 -> {
                        PlanningTableAssertion pta2 = new PlanningTableAssertion(items2);
                        pta2.assertItem("Opnieuw T1");
                        pta2.assertItem("T4");
                    });
                });

                // Now add T5 to the plan; it must be planned, but may not become Available, because Stage is still in Available state
                DiscretionaryItemAssertion t5 = pta.assertItem("T5");
                String t5stageId = t5.getParentId();
                String t5DefinitionId = t5.getDefinitionId();
                testCase.insertTestStep(new AddDiscretionaryItem(testUser, caseInstanceId, "T5", t5DefinitionId, t5stageId, "t5item"), action2 -> {
                    CaseAssertion casePlan2 = new CaseAssertion(action2);
                    TestScript.debugMessage(casePlan2);
                    casePlan2.assertLastTransition(Transition.Create, State.Active, State.Null);

                    StageAssertion stage = casePlan2.assertStage("Stage");
                    stage.assertLastTransition(Transition.Create, State.Available, State.Null);
                    stage.assertPlanItem("T5").assertState(State.Null);
                });

            });
        });

        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "T2"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            casePlan.assertTask("T1").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T2").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T3").assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertTask("T4").assertLastTransition(Transition.Start, State.Active, State.Available);

            testCase.insertTestStep(new GetDiscretionaryItems(testUser, caseInstanceId), items -> {
                PlanningTableAssertion pta = new PlanningTableAssertion(items);
                pta.assertItem("Opnieuw T1");
                pta.assertNoItem("T4");
            });
        });
        
        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "T3"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            casePlan.assertTask("T1").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T2").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T3").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T4").assertLastTransition(Transition.Start, State.Active, State.Available);
            StageAssertion stage = casePlan.assertStage("Stage");
            // Stage should have become active, and T5 should have become Active too.
            stage.assertLastTransition(Transition.Start, State.Active, State.Available);
            stage.assertPlanItem("T5").assertState(State.Active);

            testCase.insertTestStep(new GetDiscretionaryItems(testUser, caseInstanceId), items -> {
                PlanningTableAssertion pta = new PlanningTableAssertion(items);
                pta.assertItem("Opnieuw T1");
                pta.assertNoItem("T4");
            });
        });
        
        testCase.addTestStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Terminate), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            casePlan.assertLastTransition(Transition.Terminate, State.Terminated, State.Active);

            casePlan.assertTask("T1").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T2").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T3").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertTask("T4").assertLastTransition(Transition.Exit, State.Terminated, State.Active);

            testCase.insertTestStep(new GetDiscretionaryItems(testUser, caseInstanceId), items -> {
                PlanningTableAssertion pta = new PlanningTableAssertion(items);
                pta.assertItem("Opnieuw T1");
                pta.assertNoItem("T4");
            });
        });
        
        testCase.runTest();

    }
}
