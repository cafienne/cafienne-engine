/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.cmmn.test.planning;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.plan.AddDiscretionaryItem;
import org.cafienne.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import org.cafienne.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.cmmn.actorapi.response.GetDiscretionaryItemsResponse;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.TestUser;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.DiscretionaryItemAssertion;
import org.cafienne.cmmn.test.assertions.PlanningTableAssertion;
import org.junit.Test;

public class PlanningAuthorizationTest {

    private final String testName = "authorization-test";
    private final String caseInstanceId = testName;
    private final TestUser caseOwner = TestScript.getTestUser("CaseOwner");
    private final TestUser caseMember = TestScript.getTestUser("CaseMember");
    private final TestUser planner = TestScript.getTestUser("Planner", "planner");
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/planning/authorization.xml");

    @Test
    public void testPlanningAuthorization() {
        TestScript testCase = new TestScript(testName);
        CaseTeam caseTeam = TestScript.getCaseTeam(TestScript.getOwner(caseOwner), caseMember, planner);

        StartCase startCase = testCase.createCaseCommand(caseOwner, caseInstanceId, definitions, caseTeam);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();

            final String discretionaryTaskName = "PlanMe";

            testCase.insertStep(new GetDiscretionaryItems(caseOwner, caseInstanceId), step -> {
                PlanningTableAssertion pta = new PlanningTableAssertion(step);

                // Now add discretionary task to the plan
                DiscretionaryItemAssertion discretionaryTask = pta.assertItem(discretionaryTaskName);
                String stageId = discretionaryTask.getParentId();
                String definitionId = discretionaryTask.getDefinitionId();
                testCase.insertStepFails(new AddDiscretionaryItem(caseMember, caseInstanceId, "PlanMe", definitionId, stageId, "planned-item"), failure -> {
                    // Planning by caseMember should fail, but by planner it should succeed.
                    testCase.insertStep(new AddDiscretionaryItem(planner, caseInstanceId, "PlanMe", definitionId, stageId, "planned-item"), CaseAssertion::print);
                });
            });


        });


        testCase.runTest();
    }

    @Test
    public void testGetDiscretionaryItems() {
        TestScript testCase = new TestScript(testName);
        CaseTeam caseTeam = TestScript.getCaseTeam(TestScript.getOwner(caseOwner), caseMember, planner);

        StartCase startCase = testCase.createCaseCommand(caseOwner, caseInstanceId, definitions, caseTeam);
        testCase.addStep(startCase, CaseAssertion::print);

        testCase.addStep(new GetDiscretionaryItems(caseOwner, caseInstanceId), action -> {

            final String discretionaryTaskName = "PlanMe";

            PlanningTableAssertion pta = new PlanningTableAssertion(action);
            TestScript.debugMessage("Items: "+pta);
            pta.assertItems();
            DiscretionaryItemAssertion discretionaryTask = pta.assertItem(discretionaryTaskName);

            TestScript.debugMessage("PlanMe looks like "+discretionaryTask);

            // Now add discretionary task to the plan
            String stageId = discretionaryTask.getParentId();
            String definitionId = discretionaryTask.getDefinitionId();
            testCase.insertStepFails(new AddDiscretionaryItem(caseMember, caseInstanceId, "PlanMe", definitionId, stageId, "planned-item"), failure -> {
                // Planning by anonymous should fail, but by planner it should succeed.
                testCase.insertStep(new AddDiscretionaryItem(planner, caseInstanceId, "PlanMe", definitionId, stageId, "planned-item"), CaseAssertion::print);
            });
        });

        testCase.addStep(new GetDiscretionaryItems(caseOwner, caseInstanceId), response -> {
            new PlanningTableAssertion(response).assertNoItems();
            GetDiscretionaryItemsResponse items = response.getTestCommand().getActualResponse();
            TestScript.debugMessage(items.toJson());
        });

        testCase.runTest();
    }
}