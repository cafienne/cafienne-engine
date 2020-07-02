/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.*;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.DiscretionaryItemAssertion;
import org.cafienne.cmmn.test.assertions.PlanningTableAssertion;
import org.junit.Test;

public class SentryTest {
    @Test
    public void testSentry() {
        String caseName = "sentry";
        TestScript testCase = new TestScript(caseName);

        ValueMap inputs = null;
        String caseInstanceId = caseName;
        CaseDefinition definitionsDocument = TestScript.getCaseDefinition("testdefinition/sentry.xml");
        TenantUser user = TestScript.getTestUser("Anonymous");

        testCase.addStep(new StartCase(user, caseInstanceId, definitionsDocument, inputs, null), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Stage1").assertState(State.Available);
            testCase.insertStep(new GetDiscretionaryItems(user, caseInstanceId), items -> {
                new PlanningTableAssertion(items).assertItem("Disc1");
            });
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active);
        });

        testCase.addStep(new MakeCaseTransition(user, caseInstanceId, Transition.Suspend), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Suspended);
            casePlan.assertPlanItem("Item1.1").assertState(State.Suspended);
        });

        testCase.addStep(new MakeCaseTransition(user, caseInstanceId, Transition.Reactivate), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active).assertLastTransition(Transition.ParentResume);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active).assertLastTransition(Transition.ParentResume);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1.1", Transition.Complete), casePlan -> {
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
        String caseName = "sentry";
        TestScript testCase = new TestScript(caseName);

        ValueMap inputs = null;
        String caseInstanceId = caseName;
        CaseDefinition definitionsDocument = TestScript.getCaseDefinition("testdefinition/sentry.xml");
        TenantUser user = TestScript.getTestUser("Anonymous");

        testCase.addStep(new StartCase(user, caseInstanceId, definitionsDocument, inputs, null), case1 -> {
            case1.print();
            case1.assertPlanItem("Item1").assertState(State.Active);
            case1.assertPlanItem("Stage1").assertState(State.Available);

            testCase.insertStep(new GetDiscretionaryItems(user, caseInstanceId), case2 -> {
                DiscretionaryItemAssertion discItem = new PlanningTableAssertion(case2).assertItem("Disc1");
                testCase.insertStep(new AddDiscretionaryItem(user, caseInstanceId, "Disc1", discItem.getDefinitionId(), discItem.getParentId(), null), case3 -> {
                    case3.print();
                    case3.assertPlanItem("Item1").assertState(State.Active);
                    case3.assertPlanItem("Stage1").assertState(State.Available);
                    case3.assertPlanItem("Disc1").assertState(State.Available);
                });
            });
        });


        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active);
            casePlan.assertPlanItem("Disc1").assertState(State.Active);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1.1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Disc1").assertState(State.Active);
            casePlan.assertState(State.Active);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Disc1", Transition.Complete), casePlan -> {
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
