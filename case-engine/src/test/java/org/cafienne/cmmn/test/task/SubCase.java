/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.task;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.UpdateCaseFileItem;
import org.cafienne.cmmn.akka.event.plan.PlanItemCreated;
import org.cafienne.cmmn.akka.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.PingCommand;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.EmptyCaseAssertion;
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
        testCase.addTestStep(startCase, action ->
        {
            CaseAssertion mainCasePlan = new CaseAssertion(action);
            TestScript.debugMessage("Case: " + mainCasePlan);
        });

        /**
         * Complete the Task1 in MainCase
         * This should start both SubCaseTask (blocking) and NonBlockingSubCaseTask (non-blocking)
         */
        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "Task1"), action ->
        {
            CaseAssertion mainCasePlan = new CaseAssertion(action);
            TestScript.debugMessage("Main case: " + mainCasePlan);

            // Now wait until the NonBlockingSubCaseTask has been started and has reported that properly back to the engine,
            // making the corresponding planitem go into completed
            String nonBlockingSubCaseId = testCase.getEventListener().awaitPlanItemState("NonBlockingSubCaseTask", State.Completed).getPlanItemId();
            String subCaseId = testCase.getEventListener().awaitPlanItemState("SubCaseTask", State.Active).getPlanItemId();
            // The SubCaseTask may not have gone beyond Active (e.g. into Completed), test it here on the response; this actually could be a new type of event filter?
            mainCasePlan.assertTask("SubCaseTask").assertState(State.Active);

            // Now wait until we find the Item1 task in the blocking subcase.
            String item1IdInSubCase = testCase.getEventListener().waitUntil(PlanItemCreated.class, pic -> {
                return pic.getCaseInstanceId().equals(subCaseId) && pic.getPlanItemName().equals("Item1");
            }).getPlanItemId();

            String item1InSubCase = testCase.getEventListener().waitUntil(PlanItemTransitioned.class, pit ->
                       pit.getCaseInstanceId().equals(subCaseId) // Sub case
                    && pit.getPlanItemId().equals(item1IdInSubCase) // Having plan item with name "Item1" and correct id
                    && pit.getCurrentState().equals(State.Active) // in state Active
            ).getPlanItemId();


            // Suspending subCaseTask from MainCase should also suspend SubCase task
            // And the transition in SubCase task Should be ParentSuspend, but since this is async we first need to ping the sub case with a bit of delay
            testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Suspend, "SubCaseTask"), result ->
            {
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Suspended);
                testCase.getEventListener().awaitPlanItemState(item1InSubCase, Transition.ParentSuspend, State.Suspended, State.Active);
            });

            // Resume SubCaseTask from MainCase - This should also resume Item1 in SubCase
            testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Resume, "SubCaseTask"), result ->
            {
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Active);
                testCase.getEventListener().awaitPlanItemState(item1InSubCase, Transition.ParentResume, State.Active, State.Suspended);
            });

            // Complete Item1 from SubCase - This should also complete the SubCaseTask in MainCase
            testCase.addTestStep(new MakePlanItemTransition(testUser, subCaseId, null, Transition.Complete, "Item1"), completeAction ->
            {
                testCase.getEventListener().awaitPlanItemState(item1InSubCase, State.Completed);
                TestScript.debugMessage("Main case: " + mainCasePlan);
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Completed);
            });

            /**
             * Suspending NonBlockingSubCaseTask from MainCase should not suspend SubCase task, and the CasePlan of the
             * non-blocking subcase must still be Active.
             */
            testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Suspend, "NonBlockingSubCaseTask"), result ->
            {
                TestScript.debugMessage("resulting main case: " + new CaseAssertion(result));
                testCase.insertTestStep(new PingCommand(testUser, nonBlockingSubCaseId, 100), subCaseResult ->
                {
                    CaseAssertion nonBlockingSubCasePlan = new CaseAssertion(subCaseResult);
                    TestScript.debugMessage("resulting non-blocking sub case: " + nonBlockingSubCasePlan);
                    nonBlockingSubCasePlan.assertPlanItem("Item1").assertLastTransition(Transition.Start, State.Active, State.Available);
                });
            });

            /**
             * Suspend the Item1 in NonBlockingSubCase
             */
            testCase.addTestStep(new MakePlanItemTransition(testUser, nonBlockingSubCaseId, null, Transition.Suspend, "Item1"), suspendedResult ->
            {
                CaseAssertion nonBlockingSubCasePlan = new CaseAssertion(suspendedResult);
                TestScript.debugMessage("resulting non-blocking sub case: " + nonBlockingSubCasePlan);
                nonBlockingSubCasePlan.assertPlanItem("Item1").assertLastTransition(Transition.Suspend, State.Suspended, State.Active);
            });

            /**
             * Complete the subCaseTask - First resume the SubCaseTask and complete it
             */

            /**
             * Resume SubCaseTask from SubCase
             */
            testCase.addTestStep(new MakePlanItemTransition(testUser, nonBlockingSubCaseId, null, Transition.Resume, "Item1"), resumeResult ->
            {
                CaseAssertion resumeSubCasePlan = new CaseAssertion(resumeResult);
                TestScript.debugMessage("resulting non-blocking sub case: " + resumeSubCasePlan);
                resumeSubCasePlan.assertPlanItem("Item1").assertLastTransition(Transition.Resume, State.Active, State.Suspended);
            });

            /**
             * Complete Item1 from SubCase
             */
            testCase.addTestStep(new MakePlanItemTransition(testUser, nonBlockingSubCaseId, null, Transition.Complete, "Item1"), completeAction ->
            {
                CaseAssertion completeSubCasePlan = new CaseAssertion(completeAction);
                TestScript.debugMessage("NonBlockingSubCase: " + completeSubCasePlan);
                completeSubCasePlan.assertPlanItem("Item1").assertLastTransition(Transition.Complete, State.Completed, State.Active);
                TestScript.debugMessage("MainCase: " + mainCasePlan);
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
        testCase.addTestStep(new StartCase(testUser, caseInstanceId, xml, null, null), action -> {
            CaseAssertion mainCasePlan = new CaseAssertion(action);
            TestScript.debugMessage("Case: " + mainCasePlan);
        });


        // Now set some invalid data in the main case. It is acceptable data for the main case, but when passed as input parameter
        //  to the subcase, the subcase should choke in it.
        // Next, we expect the main case's task to go to Fault state.
        ValueMap invalidMainRequest = new ValueMap();
        invalidMainRequest.putRaw("aBoolean", "I ought to be boolean but i am a string");
        testCase.addTestStep(new CreateCaseFileItem(testUser, caseInstanceId, invalidMainRequest, "InvalidMainRequest"), action -> {
            TestScript.debugMessage(new CaseAssertion(action));
        });

        testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Complete, "TriggerFailingBlockingSubCaseTask"), action -> {
            testCase.getEventListener().awaitPlanItemState("TriggerFailingBlockingSubCaseTask", State.Completed);
            String subCaseId = testCase.getEventListener().awaitPlanItemState("FailingBlockingSubCaseTask", State.Active).getPlanItemId();
            testCase.getEventListener().awaitPlanItemState("FailingBlockingSubCaseTask", Transition.Fault, State.Failed, State.Active);
            CaseAssertion mainCasePlan = new CaseAssertion(action);
            TestScript.debugMessage("Case: " + mainCasePlan);

            // Now ping the sub case. It must still be "empty", that is to say, it ought to exist (because it is a PersistentActor),
            // but without a definition, because that has failed
            testCase.addTestStep(new PingCommand(testUser, subCaseId, 0), subCaseAction -> {
                CaseAssertion casePlan = new EmptyCaseAssertion(subCaseAction);
                TestScript.debugMessage("Ping responded; case is " + casePlan);
            });

            ValueMap validMainRequest = new ValueMap();
            validMainRequest.putRaw("aBoolean", false);
            testCase.addTestStep(new UpdateCaseFileItem(testUser, caseInstanceId, validMainRequest, "InvalidMainRequest"), r -> {
                // print the updated case file
                TestScript.debugMessage(new CaseAssertion(r));
            });

            testCase.addTestStep(new MakePlanItemTransition(testUser, caseInstanceId, null, Transition.Reactivate, "FailingBlockingSubCaseTask"), r -> {
                // SubCaseTask should be active
                testCase.getEventListener().awaitPlanItemState(subCaseId, State.Active);
                // Task Item1 in subcase should also be active
                testCase.getEventListener().awaitPlanItemState("Item1", State.Active);
            });
        });

        testCase.runTest();

    }
}
