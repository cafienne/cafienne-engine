/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.PlanItemAssertion;
import org.junit.Test;

/**
 * This case contains two tests on the case plan's exit criteria. The case plan exits when task Review2 is completed.
 * In the first test, everything goes normal, i.e., the first task is completed, and then the second,
 * making the case plan to normally completes before it is tried to be terminated.
 * In the second case, we immediately complete task Review2, which causes the Review task to remain active, and then
 * the case plan is not normally completed, and subsequently it is terminated, causing the Review task as well as the case plan
 * itself to go into Terminated state.
 */
public class CasePlanExitCriteria {

    private final TenantUser testUser = TestScript.getTestUser("Anonymous");
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/caseplanexitcriteria.xml");

    @Test
    public void testCasePlanExitCompletion() {
        String caseInstanceId = "CasePlanExitCriteria-CompletionTest";
        TestScript testCase = new TestScript(caseInstanceId);
        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            PlanItemAssertion reviewTask = casePlan.assertTask("Review");
            reviewTask.assertState(State.Active);
            PlanItemAssertion reviewTask2 = casePlan.assertTask("Review2");
            reviewTask2.assertState(State.Active);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Review", Transition.Complete), casePlan -> {
            PlanItemAssertion reviewTask = casePlan.assertTask("Review");
            reviewTask.assertState(State.Completed);
            PlanItemAssertion reviewTask2 = casePlan.assertTask("Review2");
            reviewTask2.assertState(State.Active);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Review2", Transition.Complete), casePlan -> {
            casePlan.assertState(State.Completed);
            PlanItemAssertion reviewTask = casePlan.assertTask("Review");
            reviewTask.assertState(State.Completed);
            PlanItemAssertion reviewTask2 = casePlan.assertTask("Review2");
            reviewTask2.assertState(State.Completed);
        });

        testCase.runTest();
    }

    @Test
    public void testCasePlanExitTermination() {
        String caseInstanceId = "CasePlanExitCriteria-TerminationTest";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            PlanItemAssertion reviewTask = casePlan.assertTask("Review");
            reviewTask.assertState(State.Active);
            PlanItemAssertion reviewTask2 = casePlan.assertTask("Review2");
            reviewTask2.assertState(State.Active);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Review2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertState(State.Terminated);
            PlanItemAssertion reviewTask = casePlan.assertTask("Review");
            reviewTask.assertState(State.Terminated);
            PlanItemAssertion reviewTask2 = casePlan.assertTask("Review2");
            reviewTask2.assertState(State.Completed);
        });
        testCase.runTest();
    }

}
