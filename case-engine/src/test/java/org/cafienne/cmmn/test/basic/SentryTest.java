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
import org.cafienne.cmmn.test.assertions.CaseAssertion;
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

        testCase.addTestStep(new StartCase(user, caseInstanceId, definitionsDocument, inputs, null), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Stage1").assertState(State.Available);
            testCase.insertTestStep(new GetDiscretionaryItems(user, caseInstanceId), items -> {
                new PlanningTableAssertion(items).assertItem("Disc1");
            });
        });

        testCase.addTestStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Complete, "Item1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active);
        });

        testCase.addTestStep(new MakeCaseTransition(user, caseInstanceId, Transition.Suspend), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Suspended);
            casePlan.assertPlanItem("Item1.1").assertState(State.Suspended);
        });

        testCase.addTestStep(new MakeCaseTransition(user, caseInstanceId, Transition.Reactivate), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active).assertLastTransition(Transition.ParentResume);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active).assertLastTransition(Transition.ParentResume);
        });

        testCase.addTestStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Complete, "Item1.1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
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

        testCase.addTestStep(new StartCase(user, caseInstanceId, definitionsDocument, inputs, null), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Stage1").assertState(State.Available);

            testCase.insertTestStep(new GetDiscretionaryItems(user, caseInstanceId), items -> {
                DiscretionaryItemAssertion discItem = new PlanningTableAssertion(items).assertItem("Disc1");
                testCase.insertTestStep(new AddDiscretionaryItem(user, caseInstanceId, "Disc1", discItem.getDefinitionId(), discItem.getParentId(), null), action2 -> {
                    CaseAssertion casePlan2 = new CaseAssertion(action2);
                    TestScript.debugMessage(casePlan2);
                    casePlan.assertPlanItem("Item1").assertState(State.Active);
                    casePlan.assertPlanItem("Stage1").assertState(State.Available);
                    casePlan.assertPlanItem("Disc1").assertState(State.Available);
                });
            });
        });


        testCase.addTestStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Complete, "Item1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active);
            casePlan.assertPlanItem("Disc1").assertState(State.Active);
        });

        testCase.addTestStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Complete, "Item1.1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Disc1").assertState(State.Active);
            casePlan.assertState(State.Active);
        });

        testCase.addTestStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Complete, "Disc1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Disc1").assertState(State.Completed);
            casePlan.assertState(State.Completed);
        });

        testCase.runTest();
    }
}
