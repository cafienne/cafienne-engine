/* 
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.casefabric.cmmn.test.basic;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.actorapi.command.plan.AddDiscretionaryItem;
import com.casefabric.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import com.casefabric.cmmn.actorapi.command.plan.MakeCaseTransition;
import com.casefabric.cmmn.actorapi.command.plan.MakePlanItemTransition;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.cmmn.test.assertions.DiscretionaryItemAssertion;
import com.casefabric.cmmn.test.assertions.PlanningTableAssertion;
import org.junit.Test;

import static com.casefabric.cmmn.test.TestScript.*;

public class TestSentry {
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/sentry.xml");

    @Test
    public void testSentry() {
        String caseInstanceId = "sentry";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Stage1").assertState(State.Available);
            testCase.insertStep(new GetDiscretionaryItems(testUser, caseInstanceId), items -> {
                new PlanningTableAssertion(items).assertItem("Disc1");
            });
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active);
        });

        testCase.addStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Suspend), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Suspended);
            casePlan.assertPlanItem("Item1.1").assertState(State.Suspended);
        });

        testCase.addStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Reactivate), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active).assertLastTransition(Transition.ParentResume);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active).assertLastTransition(Transition.ParentResume);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1.1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertState(State.Completed);
        });

        testCase.runTest();
    }

    @Test
    public void testSentryOnDiscretionary() {
        String caseInstanceId = "sentryOnDiscretionary";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, case1 -> {
            case1.print();
            case1.assertPlanItem("Item1").assertState(State.Active);
            case1.assertPlanItem("Stage1").assertState(State.Available);

            testCase.insertStep(new GetDiscretionaryItems(testUser, caseInstanceId), case2 -> {
                DiscretionaryItemAssertion discItem = new PlanningTableAssertion(case2).assertItem("Disc1");
                testCase.insertStep(new AddDiscretionaryItem(testUser, caseInstanceId, "Disc1", discItem.getDefinitionId(), discItem.getParentId(), null), case3 -> {
                    case3.print();
                    case3.assertPlanItem("Item1").assertState(State.Active);
                    case3.assertPlanItem("Stage1").assertState(State.Available);
                    case3.assertPlanItem("Disc1").assertState(State.Available);
                });
            });
        });


        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active);
            casePlan.assertPlanItem("Disc1").assertState(State.Active);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Item1.1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Disc1").assertState(State.Active);
            casePlan.assertState(State.Active);
        });

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "Disc1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Disc1").assertState(State.Completed);
            casePlan.assertState(State.Completed);
        });

        testCase.runTest();
    }
}
