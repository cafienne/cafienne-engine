/* 
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.casefabric.cmmn.test.basic;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.actorapi.command.plan.MakePlanItemTransition;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.cmmn.test.TestScript;
import org.junit.Test;

import static com.casefabric.cmmn.test.TestScript.*;

public class TestRequiredRule {
    @Test
    public void testRequiredRule() {
        String caseInstanceId = "requiredrule";
        TestScript testCase = new TestScript(caseInstanceId);

        CaseDefinition definitions = loadCaseDefinition("testdefinition/requiredrule.xml");

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.2").assertState(State.Available);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1.1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Active);
            // Auto completion for Stage1 must have triggered, but since Item1.2 is required, stage should still be Active
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);

        });

        // Now complete Item2 multiple times. It is not supposed to repeat more than 10 times, it says in the definition
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1.2", Transition.Suspend), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Suspended);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1.2", Transition.Resume), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Active).assertLastTransition(Transition.Resume);
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1.2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Completed);
            // Auto completion for Stage1 must be triggered and complete the stage. Item1.3 should still be Available
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            // It should be possible to complete Item1, but Item1.3 should not longer listen to it.
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.runTest();
    }
}
