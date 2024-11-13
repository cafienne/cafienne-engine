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

public class TestRepeatRule {

    @Test
    public void testRepeatRule() {
        String caseInstanceId = "repeatrule";
        TestScript testCase = new TestScript(caseInstanceId);

        CaseDefinition definitions = loadCaseDefinition("testdefinition/repeatrule.xml");

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, case1 -> {
            case1.print();
            case1.assertPlanItem("Item1").assertState(State.Active);
            case1.assertPlanItems("Item2").filter(State.Available).assertSize(1);
        });

        // Now complete Item1. This should activate Item2 for the first time.
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(0);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        // Now complete Item2 multiple times. It is not supposed to repeat more than 10 times, it says in the definition
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(1);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(2);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(3);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(4);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(5);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(6);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(7);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(8);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(9);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
        });

        // It should stop repeating after the 10th item is put inside
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(10);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(1);
            casePlan.assertPlanItems("Item2").filter(State.Available).assertSize(0);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItems("Item2").filter(State.Completed).assertSize(11);
            casePlan.assertPlanItems("Item2").filter(State.Active).assertSize(0);
            casePlan.assertPlanItems("Item2").filter(State.Available).assertSize(0);
        });

        // keeping completing should not lead to more items ;)
        testCase.assertStepFails(new MakePlanItemTransition(testUser, caseInstanceId, "Item2", Transition.Complete));

        testCase.runTest();
    }
}
