/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.task;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.UpdateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.Path;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.json.ValueMap;
import org.junit.Test;

import static org.cafienne.cmmn.test.TestScript.*;

public class TestSubCase {
    @Test
    public void testSubCase() {
        String caseInstanceId = "SubCaseTest";
        TestScript testCase = new TestScript("SubCase");

        CaseDefinition definitions = loadCaseDefinition("testdefinition/task/subcase.xml");

        /**
         * Start the MainCase
         */
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, action -> action.print());

        /**
         * Complete the Task1 in MainCase
         * This should start both SubCaseTask (blocking) and NonBlockingSubCaseTask (non-blocking)
         */
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Task1", Transition.Complete), mainCasePlan ->
        {
            mainCasePlan.print();

            // Now wait until the NonBlockingSubCaseTask has been started and has reported that properly back to the engine,
            // making the corresponding planitem go into completed
            String nonBlockingSubCaseId = testCase.getEventListener().awaitPlanItemState("NonBlockingSubCaseTask", State.Completed).getPlanItemId();
            String subCaseId = testCase.getEventListener().awaitPlanItemState("SubCaseTask", State.Active).getPlanItemId();
            // The SubCaseTask may not have gone beyond Active (e.g. into Completed), test it here on the response; this actually could be a new type of event filter?
            mainCasePlan.assertTask("SubCaseTask").assertState(State.Active);

            // Now wait until we find the Item1 task in the blocking subcase.
            String item1IdInSubCase = testCase.getEventListener().waitUntil(PlanItemCreated.class, pic -> pic.getCaseInstanceId().equals(subCaseId) && pic.getPlanItemName().equals("Item1")).getPlanItemId();

            String item1InSubCase = testCase.getEventListener().waitUntil(PlanItemTransitioned.class, pit ->
                    pit.getCaseInstanceId().equals(subCaseId) // Sub case
                            && pit.getPlanItemId().equals(item1IdInSubCase) // Having plan item with name "Item1" and correct id
                            && pit.getCurrentState().equals(State.Active) // in state Active
            ).getPlanItemId();


            // Suspending subCaseTask from MainCase should also suspend SubCase task
            // And the transition in SubCase task Should be ParentSuspend, but since this is async we first need to ping the sub case with a bit of delay
            testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "SubCaseTask", Transition.Suspend), result ->
            {
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Suspended);
                testCase.getEventListener().awaitPlanItemState(item1InSubCase, Transition.ParentSuspend, State.Suspended, State.Active);
            });

            // Resume SubCaseTask from MainCase - This should also resume Item1 in SubCase
            testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "SubCaseTask", Transition.Resume), result ->
            {
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Active);
                testCase.getEventListener().awaitPlanItemState(item1InSubCase, Transition.ParentResume, State.Active, State.Suspended);
            });

            // Complete Item1 from SubCase - This should also complete the SubCaseTask in MainCase
            testCase.addStep(new MakePlanItemTransition(testUser, subCaseId, "Item1", Transition.Complete), completeAction -> {
                testCase.getEventListener().awaitPlanItemState(item1InSubCase, State.Completed);
                TestScript.debugMessage("Main case: " + mainCasePlan); // This will not print latest state, better freshly get that case and then print
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Completed);
            });

            /**
             * Suspending NonBlockingSubCaseTask from MainCase should not suspend SubCase task, and the CasePlan of the
             * non-blocking subcase must still be Active.
             */
            testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "NonBlockingSubCaseTask", Transition.Suspend), suspendedMainCase -> {
                TestScript.debugMessage("resulting main case: " + suspendedMainCase);
                testCase.insertStep(testCase.createPingCommand(testUser, nonBlockingSubCaseId, 100), nonBlockingSubCasePlan -> {
                    TestScript.debugMessage("resulting non-blocking sub case: " + nonBlockingSubCasePlan);
                    nonBlockingSubCasePlan.assertPlanItem("Item1").assertLastTransition(Transition.Start, State.Active, State.Available);
                });
            });

            /**
             * Suspend the Item1 in NonBlockingSubCase
             */
            testCase.addStep(new MakePlanItemTransition(testUser, nonBlockingSubCaseId, "Item1", Transition.Suspend), nonBlockingSubCasePlan -> {
                TestScript.debugMessage("resulting non-blocking sub case: " + nonBlockingSubCasePlan);
                nonBlockingSubCasePlan.assertPlanItem("Item1").assertLastTransition(Transition.Suspend, State.Suspended, State.Active);
            });

            /**
             * Complete the subCaseTask - First resume the SubCaseTask and complete it
             */

            /**
             * Resume SubCaseTask from SubCase
             */
            testCase.addStep(new MakePlanItemTransition(testUser, nonBlockingSubCaseId, "Item1", Transition.Resume), resumeSubCasePlan -> {
                TestScript.debugMessage("resulting non-blocking sub case: " + resumeSubCasePlan);
                resumeSubCasePlan.assertPlanItem("Item1").assertLastTransition(Transition.Resume, State.Active, State.Suspended);
            });

            /**
             * Complete Item1 from SubCase
             */
            testCase.addStep(new MakePlanItemTransition(testUser, nonBlockingSubCaseId, "Item1", Transition.Complete), completedSubCasePlan -> {
                TestScript.debugMessage("NonBlockingSubCase: " + completedSubCasePlan);
                completedSubCasePlan.assertPlanItem("Item1").assertLastTransition(Transition.Complete, State.Completed, State.Active);
                TestScript.debugMessage("MainCase: " + mainCasePlan); // Probably better to ping and fetch again
            });
        });

        testCase.runTest();
    }

    @Test
    public void testSubCaseTermination() {
        String caseInstanceId = "SubCaseTest";
        TestScript testCase = new TestScript("SubCase");

        CaseDefinition definitions = loadCaseDefinition("testdefinition/task/subcase.xml");

        /**
         * Start the MainCase
         */
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, action -> action.print());

        /**
         * Complete the Task1 in MainCase
         * This should start both SubCaseTask (blocking) and NonBlockingSubCaseTask (non-blocking)
         */
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Task1", Transition.Complete), mainCasePlan ->
        {
            mainCasePlan.print();

            // Get a hold of sub case id and assert it is active.
            String subCaseId = testCase.getEventListener().awaitPlanItemState("SubCaseTask", State.Active).getPlanItemId();
            mainCasePlan.assertTask("SubCaseTask").assertState(State.Active);

            // Now wait until we find the Item1 task in the blocking subcase.
            String item1IdInSubCase = testCase.getEventListener().waitUntil(PlanItemCreated.class, pic -> pic.getCaseInstanceId().equals(subCaseId) && pic.getPlanItemName().equals("Item1")).getPlanItemId();

            // And check that it is not only Created, but also Active
            String item1InSubCase = testCase.getEventListener().waitUntil(PlanItemTransitioned.class, pit ->
                    pit.getCaseInstanceId().equals(subCaseId) // Sub case
                            && pit.getPlanItemId().equals(item1IdInSubCase) // Having plan item with name "Item1" and correct id
                            && pit.getCurrentState().equals(State.Active) // in state Active
            ).getPlanItemId();

            // Terminating subCaseTask from MainCase should also terminate SubCase and task in it
            // And the transition in SubCase task Should be Exit
            testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "SubCaseTask", Transition.Terminate), result ->
            {
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Terminated);
                testCase.getEventListener().awaitPlanItemState(item1InSubCase, Transition.Exit, State.Terminated, State.Active);
            });
        });

        testCase.runTest();
    }

    @Test
    public void testFailingSubCase() {
        String caseInstanceId = "SubCaseTest";
        TestScript testCase = new TestScript("SubCase");

        CaseDefinition definitions = loadCaseDefinition("testdefinition/task/subcase.xml");

        /**
         * Start the MainCase
         */
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, mainCasePlan -> mainCasePlan.print());


        // Now set some invalid data in the main case. It is acceptable data for the main case, but when passed as input parameter
        //  to the subcase, the subcase should choke in it.
        // Next, we expect the main case's task to go to Fault state.
        ValueMap invalidMainRequest = new ValueMap();
        invalidMainRequest.plus("aBoolean", "I ought to be boolean but i am a string");
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, invalidMainRequest, new Path("InvalidMainRequest")), action -> action.print());

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "TriggerFailingBlockingSubCaseTask", Transition.Complete), mainCasePlan -> {
            testCase.getEventListener().awaitPlanItemState("TriggerFailingBlockingSubCaseTask", State.Completed);
            String subCaseId = testCase.getEventListener().awaitPlanItemState("FailingBlockingSubCaseTask", State.Active).getPlanItemId();
            testCase.getEventListener().awaitPlanItemState("FailingBlockingSubCaseTask", Transition.Fault, State.Failed, State.Active);
            mainCasePlan.print();

            ValueMap validMainRequest = new ValueMap();
            validMainRequest.plus("aBoolean", false);
            testCase.addStep(new UpdateCaseFileItem(testUser, caseInstanceId, validMainRequest, new Path("InvalidMainRequest")), r -> r.print()); // print the updated case file

            testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "FailingBlockingSubCaseTask", Transition.Reactivate), r -> {
                // SubCaseTask should be active
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Active);
                // Task Item1 in subcase should also be active
                testCase.getEventListener().awaitPlanItemState("Item1", State.Active);
            });
        });

        testCase.runTest();

    }
}
