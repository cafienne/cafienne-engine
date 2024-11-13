/*
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.casefabric.cmmn.test.planning;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.actorapi.command.plan.AddDiscretionaryItem;
import com.casefabric.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import com.casefabric.cmmn.actorapi.command.team.CaseTeam;
import com.casefabric.cmmn.actorapi.response.GetDiscretionaryItemsResponse;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.cmmn.test.TestUser;
import com.casefabric.cmmn.test.assertions.CaseAssertion;
import com.casefabric.cmmn.test.assertions.DiscretionaryItemAssertion;
import com.casefabric.cmmn.test.assertions.PlanningTableAssertion;
import org.junit.Test;

import static com.casefabric.cmmn.test.TestScript.*;

public class PlanningAuthorizationTest {

    private final String testName = "authorization-test";
    private final String caseInstanceId = testName;
    private final TestUser caseOwner = createTestUser("CaseOwner");
    private final TestUser caseMember = createTestUser("CaseMember");
    private final TestUser planner = createTestUser("Planner", "planner");
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/planning/authorization.xml");

    @Test
    public void testPlanningAuthorization() {
        TestScript testCase = new TestScript(testName);
        CaseTeam caseTeam = createCaseTeam(TestScript.createOwner(caseOwner), caseMember, planner);

        StartCase startCase = createCaseCommand(caseOwner, caseInstanceId, definitions, caseTeam);
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
        CaseTeam caseTeam = createCaseTeam(TestScript.createOwner(caseOwner), caseMember, planner);

        StartCase startCase = createCaseCommand(caseOwner, caseInstanceId, definitions, caseTeam);
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