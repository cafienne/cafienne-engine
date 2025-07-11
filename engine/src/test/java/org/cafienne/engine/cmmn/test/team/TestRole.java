/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.engine.cmmn.test.team;

import org.cafienne.engine.cmmn.actorapi.command.StartCase;
import org.cafienne.engine.cmmn.actorapi.command.plan.MakeCaseTransition;
import org.cafienne.engine.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.engine.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.engine.cmmn.definition.CaseDefinition;
import org.cafienne.engine.cmmn.instance.Transition;
import org.cafienne.engine.cmmn.instance.TransitionDeniedException;
import org.cafienne.engine.cmmn.test.TestScript;
import org.cafienne.engine.cmmn.test.TestUser;
import org.cafienne.engine.cmmn.test.assertions.CaseAssertion;
import org.junit.Test;

import static org.cafienne.engine.cmmn.test.TestScript.*;

public class TestRole {

    private final String testName = "roles";
    private final String caseInstanceId = testName;
    private final TestUser admin = createTestUser("Admin", "Admin");
    private final TestUser employee = createTestUser("Employee", "Employee");
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/team/roles.xml");

    @Test
    public void testRolesBasedAuthorization() {
        TestScript testCase = new TestScript(testName);
        CaseTeam caseTeam = createCaseTeam(TestScript.createOwner(testUser), admin, employee);
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, caseTeam);

        testCase.addStep(startCase, CaseAssertion::print);

        // With Role admin
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, "UserEvent1", Transition.Occur), CaseAssertion::print);

        // With Role admin
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, "Item1", Transition.Complete), CaseAssertion::print);

        // With anonymous
        testCase.addStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Suspend), CaseAssertion::print);

        testCase.addStep(new MakeCaseTransition(testUser, caseInstanceId, Transition.Reactivate), CaseAssertion::print);

        testCase.assertStepFails(new MakePlanItemTransition(employee, caseInstanceId, "Item1.1", Transition.Complete),
                failure -> failure.assertException(TransitionDeniedException.class, "You do not have the permission to perform the task"));

        // With admin user
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, "Item1.1", Transition.Complete), CaseAssertion::print);

        testCase.addStep(new MakeCaseTransition(admin, caseInstanceId, Transition.Complete), CaseAssertion::print);

        testCase.runTest();
    }
}
