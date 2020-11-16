/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.task;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.UpdateCaseFileItem;
import org.cafienne.cmmn.akka.event.CaseDefinitionApplied;
import org.cafienne.cmmn.akka.event.plan.PlanItemCreated;
import org.cafienne.cmmn.akka.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.PingCommand;
import org.cafienne.cmmn.test.TestScript;
import org.junit.Test;

public class SubCase {
    @Test
    public void testSubCase() {
        String caseInstanceId = "SubCaseTest";
        TestScript testCase = new TestScript("SubCase");

        CaseDefinition xml = TestScript.getCaseDefinition("testdefinition/task/subcase.xml");
        TenantUser testUser = TestScript.getTestUser("Anonymous");

        /**
         * Start the MainCase
         */
        StartCase startCase = new StartCase(testUser, caseInstanceId, xml, null, null);
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
                testCase.insertStep(new PingCommand(testUser, nonBlockingSubCaseId, 100), nonBlockingSubCasePlan -> {
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

        CaseDefinition xml = TestScript.getCaseDefinition("testdefinition/task/subcase.xml");
        TenantUser testUser = TestScript.getTestUser("Anonymous");

        /**
         * Start the MainCase
         */
        StartCase startCase = new StartCase(testUser, caseInstanceId, xml, null, null);
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

        CaseDefinition xml = TestScript.getCaseDefinition("testdefinition/task/subcase.xml");
        TenantUser testUser = TestScript.getTestUser("Anonymous");

        /**
         * Start the MainCase
         */
        testCase.addStep(new StartCase(testUser, caseInstanceId, xml, null, null), mainCasePlan -> mainCasePlan.print());


        // Now set some invalid data in the main case. It is acceptable data for the main case, but when passed as input parameter
        //  to the subcase, the subcase should choke in it.
        // Next, we expect the main case's task to go to Fault state.
        ValueMap invalidMainRequest = new ValueMap();
        invalidMainRequest.putRaw("aBoolean", "I ought to be boolean but i am a string");
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, new Path("InvalidMainRequest"), invalidMainRequest), action -> action.print());

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "TriggerFailingBlockingSubCaseTask", Transition.Complete), mainCasePlan -> {
            testCase.getEventListener().awaitPlanItemState("TriggerFailingBlockingSubCaseTask", State.Completed);
            String subCaseId = testCase.getEventListener().awaitPlanItemState("FailingBlockingSubCaseTask", State.Active).getPlanItemId();
            testCase.getEventListener().awaitPlanItemState("FailingBlockingSubCaseTask", Transition.Fault, State.Failed, State.Active);
            mainCasePlan.print();

            // Now ping the sub case. It must still be "empty", that is to say, it ought to exist (because it is a PersistentActor),
            // but without a definition, because that has failed
            testCase.addStep(new PingCommand(testUser, subCaseId, 0), casePlan -> {
                if (!testCase.getEventListener().getEvents().filter(subCaseId).filter(CaseDefinitionApplied.class).getEvents().isEmpty()) {
                    throw new AssertionError("Case has a definition, but it is not expected to have one");
                }
                TestScript.debugMessage("Ping responded; case is " + casePlan);
            });

            ValueMap validMainRequest = new ValueMap();
            validMainRequest.putRaw("aBoolean", false);
            testCase.addStep(new UpdateCaseFileItem(testUser, caseInstanceId, new Path("InvalidMainRequest"), validMainRequest), r -> r.print()); // print the updated case file

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
