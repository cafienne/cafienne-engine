/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.PlanItemAssertion;
import org.junit.Test;

public class SentryRef {
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/sentryRef.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testSentryRef() {
        String caseInstanceId = "SentryRefTest";
        TestScript testCase = new TestScript("sentryRef");

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, action -> action.print());

        MakePlanItemTransition completeTask1 = new MakePlanItemTransition(testUser, caseInstanceId, "Task_1", Transition.Complete);
        testCase.addStep(completeTask1, casePlan -> {
            casePlan.print();
            casePlan.assertLastTransition(Transition.Create, State.Active, State.Null);

            PlanItemAssertion stage2 = casePlan.assertStage("Stage_2");
            stage2.assertState(State.Active);
            PlanItemAssertion stage3 = casePlan.assertStage("Stage_3");
            stage3.assertState(State.Available);
        });

        testCase.runTest();
    }
}
