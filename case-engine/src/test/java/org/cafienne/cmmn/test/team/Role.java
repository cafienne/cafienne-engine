/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.team;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.plan.MakeCaseTransition;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.TransitionDeniedException;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.TestUser;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.junit.Test;

public class Role {

    private final String testName = "roles";
    private final String caseInstanceId = testName;
    private final TestUser anonymous = TestScript.getTestUser("Anonymous");
    private final TestUser admin = TestScript.getTestUser("Admin", "Admin");
    private final TestUser employee = TestScript.getTestUser("Employee", "Employee");
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/team/roles.xml");

    @Test
    public void testRolesBasedAuthorization() {
        TestScript testCase = new TestScript(testName);
        CaseTeam caseTeam = TestScript.getCaseTeam(TestScript.getOwner(anonymous), admin, employee);
        StartCase startCase = testCase.createCaseCommand(anonymous, caseInstanceId, definitions, caseTeam);

        testCase.addStep(startCase, CaseAssertion::print);

        // With Role admin
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, "UserEvent1", Transition.Occur), CaseAssertion::print);

        // With Role admin
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, "Item1", Transition.Complete), CaseAssertion::print);

        // With anonymous
        testCase.addStep(new MakeCaseTransition(anonymous, caseInstanceId, Transition.Suspend), CaseAssertion::print);

        testCase.addStep(new MakeCaseTransition(anonymous, caseInstanceId, Transition.Reactivate), CaseAssertion::print);

        testCase.assertStepFails(new MakePlanItemTransition(employee, caseInstanceId, "Item1.1", Transition.Complete),
                failure -> failure.assertException(TransitionDeniedException.class, "You do not have the permission to perform the task"));

        // With admin user
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, "Item1.1", Transition.Complete), CaseAssertion::print);

        testCase.addStep(new MakeCaseTransition(admin, caseInstanceId, Transition.Complete), CaseAssertion::print);

        testCase.runTest();
    }
}
