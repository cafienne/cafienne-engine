/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.cmmn.test.planning;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.AddDiscretionaryItem;
import org.cafienne.cmmn.akka.command.GetDiscretionaryItems;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.response.GetDiscretionaryItemsResponse;
import org.cafienne.cmmn.akka.command.team.CaseTeam;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.DiscretionaryItemAssertion;
import org.cafienne.cmmn.test.assertions.FailureAssertion;
import org.cafienne.cmmn.test.assertions.PlanningTableAssertion;
import org.junit.Test;

public class PlanningAuthorizationTest {

    private final String testName = "authorization-test";
    private final String caseInstanceId = testName;
    private final TenantUser anonymous = TestScript.getTestUser("Anonymous");
    private final TenantUser planner = TestScript.getTestUser("Planner", "planner");
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/planning/authorization.xml");

    @Test
    public void testPlanningAuthorization() {
        TestScript testCase = new TestScript(testName);
        CaseTeam caseTeam = TestScript.getCaseTeam(anonymous, planner);

        testCase.addTestStep(new StartCase(anonymous, caseInstanceId, definitions, null, caseTeam), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);

            final String discretionaryTaskName = "PlanMe";

            testCase.insertTestStep(new GetDiscretionaryItems(anonymous, caseInstanceId), step -> {
                PlanningTableAssertion pta = new PlanningTableAssertion(step);

                // Now add discretionary task to the plan
                DiscretionaryItemAssertion discretionaryTask = pta.assertItem(discretionaryTaskName);
                String stageId = discretionaryTask.getParentId();
                String definitionId = discretionaryTask.getDefinitionId();
                testCase.insertTestStep(new AddDiscretionaryItem(anonymous, caseInstanceId, "PlanMe", definitionId, stageId, "planned-item"), action2 -> {
                    // Planning by anonymous should fail.
                    FailureAssertion fail = new FailureAssertion(action2);

                    testCase.insertTestStep(new AddDiscretionaryItem(planner, caseInstanceId, "PlanMe", definitionId, stageId, "planned-item"), action3 -> {
                        // Planning by planner should not fail
                        CaseAssertion lastPlan = new CaseAssertion(action);
                        TestScript.debugMessage(lastPlan);
                    });
                });
            });


        });


        testCase.runTest();
    }

    @Test
    public void testGetDiscretionaryItems() {
        TestScript testCase = new TestScript(testName);
        CaseTeam caseTeam = TestScript.getCaseTeam(anonymous, planner);

        testCase.addTestStep(new StartCase(anonymous, caseInstanceId, definitions, null, caseTeam), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        testCase.addTestStep(new GetDiscretionaryItems(anonymous, caseInstanceId), action -> {

            final String discretionaryTaskName = "PlanMe";

            PlanningTableAssertion pta = new PlanningTableAssertion(action);
            TestScript.debugMessage("Items: "+pta);
            pta.assertItems();
            DiscretionaryItemAssertion discItem = pta.assertItem(discretionaryTaskName);

            TestScript.debugMessage("PlanMe looks like "+discItem);

            // Now add discretionary task to the plan
            DiscretionaryItemAssertion discretionaryTask = discItem;
            String stageId = discretionaryTask.getParentId();
            String definitionId = discretionaryTask.getDefinitionId();
            testCase.insertTestStep(new AddDiscretionaryItem(anonymous, caseInstanceId, "PlanMe", definitionId, stageId, "planned-item"), action2 -> {
                // Planning by anonymous should fail.
                FailureAssertion fail = new FailureAssertion(action2);

                testCase.insertTestStep(new AddDiscretionaryItem(planner, caseInstanceId, "PlanMe", definitionId, stageId, "planned-item"), action3 -> {
                    // Planning by planner should not fail
                    CaseAssertion lastPlan = new CaseAssertion(action);
                    TestScript.debugMessage(lastPlan);
                });
            });
        });

        testCase.addTestStep(new GetDiscretionaryItems(anonymous, caseInstanceId), response -> {
            new PlanningTableAssertion(response).assertNoItems();
            GetDiscretionaryItemsResponse items = response.getActualResponse();
            TestScript.debugMessage(items.getItems());
        });

        testCase.runTest();
    }
}