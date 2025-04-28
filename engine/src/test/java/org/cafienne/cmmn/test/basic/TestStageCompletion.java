/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.TestScript;
import org.junit.Test;

import static org.cafienne.cmmn.test.TestScript.*;

public class TestStageCompletion {
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/stageCompletion.xml");

    @Test
    public void testStage1Completion() {
        String caseInstanceId = "Stage1_CompletionTest";
        TestScript testCase = new TestScript(caseInstanceId);

        // Case contains a stage with a required Task1; completing the stage can only be done if Task1 is in semi-terminal state.
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertStage("Stage1").assertState(State.Available);
            casePlan.assertStage("Stage2").assertState(State.Available);
            casePlan.assertStage("Stage3").assertState(State.Available);
        });

        // Trying to complete the stage should not change anything
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId, "TriggerStage1", Transition.Occur), casePlan -> {
            casePlan.assertStage("Stage1").assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertStage("Stage1").assertPlanItem("Task1").assertState(State.Active);
            casePlan.assertStage("Stage1").assertPlanItem("Task2").assertState(State.Available);
            // Other stages should remain available
            casePlan.assertStage("Stage2").assertState(State.Available);
            casePlan.assertStage("Stage3").assertState(State.Available);
        });

        // Trying to complete the stage should not succeed, since the stage still has active children
        testCase.assertStepFails(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId, "Stage1", Transition.Complete));

        // Let's complete Task1
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId, "Task1", Transition.Complete), casePlan -> {
            casePlan.assertStage("Stage1").assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertStage("Stage1").assertPlanItem("Task1").assertState(State.Completed);
            casePlan.assertStage("Stage1").assertPlanItem("Task2").assertState(State.Available);
        });

        // Trying to complete the stage should now terminate the children
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId, "Stage1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertStage("Stage1").assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertStage("Stage1").assertPlanItem("Task1").assertState(State.Completed);
            casePlan.assertStage("Stage1").assertPlanItem("Task2").assertState(State.Available);
        });

        testCase.runTest();
    }


    @Test
    public void testStage2Completion() {
        final String caseInstanceId = "Stage2_CompletionTest";
        final String mainStageName = "Stage2";
        final TestScript testCase = new TestScript(caseInstanceId);

        // Case contains a stage with a required Task1; completing the stage can only be done if Task1 is in semi-terminal state.
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertStage("Stage1").assertState(State.Available);
            casePlan.assertStage(mainStageName).assertState(State.Available);
            casePlan.assertStage("Stage3").assertState(State.Available);
        });

        // Trying to complete the stage should not change anything
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId, "TriggerStage2", Transition.Occur), casePlan -> {
            casePlan.assertStage(mainStageName).assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertStage(mainStageName).assertPlanItem("Task3").assertState(State.Active);
            // Other stages should remain available
            casePlan.assertStage("Stage1").assertState(State.Available);
            casePlan.assertStage("Stage3").assertState(State.Available);
        });

        // Completing Task3 should not complete the stage
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId, "Task3", Transition.Complete), casePlan -> {
            casePlan.assertStage(mainStageName).assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertStage(mainStageName).assertPlanItem("Task3").assertState(State.Completed);
        });

        // Let's try to manually complete the stage, it should allow for it
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId, mainStageName, Transition.Complete), casePlan -> {
            casePlan.assertStage(mainStageName).assertLastTransition(Transition.Complete, State.Completed, State.Active);
        });

        testCase.runTest();
    }


    @Test
    public void testStage3Completion() {
        final String caseInstanceId = "Stage3_CompletionTest";
        final String mainStageName = "Stage3";
        final TestScript testCase = new TestScript(caseInstanceId);

        // Case contains a stage with a required Task1; completing the stage can only be done if Task1 is in semi-terminal state.
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertStage("Stage1").assertState(State.Available);
            casePlan.assertStage("Stage2").assertState(State.Available);
            casePlan.assertStage(mainStageName).assertState(State.Available);
        });

        // Trying to complete the stage should not change anything
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId, "TriggerStage3", Transition.Occur), casePlan -> {
            casePlan.assertStage(mainStageName).assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertStage(mainStageName).assertPlanItem("Task5").assertState(State.Active);
            // Other stages should remain available
            casePlan.assertStage("Stage1").assertState(State.Available);
            casePlan.assertStage("Stage2").assertState(State.Available);
        });

        // Completing Task5 should ALSO complete the stage
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, caseInstanceId,"Task5", Transition.Complete), casePlan -> {
            casePlan.assertStage(mainStageName).assertLastTransition(Transition.Complete, State.Completed, State.Active);
            casePlan.assertStage(mainStageName).assertPlanItem("Task5").assertState(State.Completed);
            // As a matter of fact, even the case itself must be in completed stage, since the case plan has autoComplete==true
            //  Stage1 and Stage2 then should still be in Available stage.
            casePlan.assertState(State.Completed);
            casePlan.assertStage("Stage1").assertState(State.Available);
            casePlan.assertStage("Stage2").assertState(State.Available);
        });

        testCase.runTest();
    }
}
