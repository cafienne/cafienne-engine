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
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.test.TestScript;
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

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1.1", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Active);
            // Auto completion for Stage1 must have triggered, but since Item1.2 is required, stage should still be Active
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);

        });

        // Now complete Item2 multiple times. It is not supposed to repeat more than 10 times, it says in the definition
        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1.2", Transition.Suspend), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Suspended);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1.2", Transition.Resume), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Active).assertLastTransition(Transition.Resume);
            casePlan.assertPlanItem("Stage1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1.2", Transition.Complete), casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("Item1").assertState(State.Active);
            casePlan.assertPlanItem("Item1.1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.2").assertState(State.Completed);
            // Auto completion for Stage1 must be triggered and complete the stage. Item1.3 should still be Available
            casePlan.assertPlanItem("Stage1").assertState(State.Completed);
            casePlan.assertPlanItem("Item1.3").assertState(State.Available);
        });

        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "Item1", Transition.Complete), casePlan -> {
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
