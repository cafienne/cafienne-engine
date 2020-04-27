/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.basic;

import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.akka.actor.identity.TenantUser;
import org.junit.Test;

public class RequiredRule {
    @Test
    public void testRequiredRule() {
        String caseName = "requiredrule";
        TestScript testCase = new TestScript(caseName);

        ValueMap inputs = null;
        String caseInstanceId = caseName;
        CaseDefinition definitionsDocument = TestScript.getCaseDefinition("testdefinition/requiredrule.xml");
        TenantUser user = TestScript.getTestUser("Anonymous");

        testCase.addStep(new StartCase(user, caseInstanceId, definitionsDocument, inputs, null), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.2").assertState(State.Available);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Complete, "Item1.1"), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Active);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);

        });

        // Now complete Item2 multiple times. It is not supposed to repeat more than 10 times, it says in the definition
        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Suspend, "Item1.2"), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Suspended);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Resume, "Item1.2"), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Active).assertLastTransition(Transition.Resume);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Complete, "Item1.2"), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.3").assertState(State.Terminated);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, null, Transition.Complete, "Item1"), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.3").assertState(State.Terminated);
        });

        testCase.runTest();
    }
}
