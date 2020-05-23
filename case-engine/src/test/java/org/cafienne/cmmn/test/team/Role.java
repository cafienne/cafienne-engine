/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.team;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.MakeCaseTransition;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.team.*;
import org.cafienne.cmmn.akka.event.team.TeamMemberRemoved;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.TransitionDeniedException;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.junit.Test;

public class Role {

    private final String testName = "roles";
    private final String caseInstanceId = testName;
    private final TenantUser anonymous = TestScript.getTestUser("Anonymous");
    private final TenantUser admin = TestScript.getTestUser("Admin", "Admin");
    private final TenantUser employee = TestScript.getTestUser("Employee", "Employee");

    @Test
    public void testRolesBasedAuthorization() {
        TestScript testCase = new TestScript(testName);
        CaseTeam caseTeam = TestScript.getCaseTeam(anonymous, admin, employee);

        testCase.addStep(new StartCase(anonymous, caseInstanceId, TestScript.getCaseDefinition("testdefinition/team/roles.xml"), null, caseTeam), casePlan -> casePlan.print());

        // With Role admin
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, null, Transition.Occur, "UserEvent1"), casePlan -> casePlan.print());

        // With Role admin
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, null, Transition.Complete, "Item1"), casePlan -> casePlan.print());

        // With anonymous
        testCase.addStep(new MakeCaseTransition(anonymous, caseInstanceId, Transition.Suspend), casePlan -> casePlan.print());

        testCase.addStep(new MakeCaseTransition(anonymous, caseInstanceId, Transition.Reactivate), casePlan -> casePlan.print());

        testCase.assertStepFails(new MakePlanItemTransition(employee, caseInstanceId, null, Transition.Complete, "Item1.1"),
                failure -> failure.assertException(TransitionDeniedException.class, "You do not have the permission to perform the task"));

        // With admin user
        testCase.addStep(new MakePlanItemTransition(admin, caseInstanceId, null, Transition.Complete, "Item1.1"), casePlan -> casePlan.print());

        testCase.addStep(new MakeCaseTransition(admin, caseInstanceId, Transition.Complete), casePlan -> casePlan.print());

        testCase.runTest();
    }

    @Test
    public void testMutexAndSingletonRoles() {

        TestScript testCase = new TestScript(testName);

        // Start a case without a team;
        testCase.addStep(new StartCase(anonymous, caseInstanceId, TestScript.getCaseDefinition("testdefinition/team/roles.xml"), null, null), casePlan -> casePlan.print());

        // Now create a team;
        CaseTeamMember user1 = new CaseTeamMember("user1", "Admin", "Employee");
        CaseTeamMember user2 = new CaseTeamMember("user2", "Manager");
        CaseTeam caseTeam = new CaseTeam(user1, user2);
        testCase.addStep(new SetCaseTeam(admin, caseInstanceId, caseTeam), casePlan -> casePlan.print());

        // Fail to add a user with conflicting roles.
        CaseTeamMember user3a = new CaseTeamMember("user3", "Admin", "Manager");
        testCase.assertStepFails(new PutTeamMember(admin, caseInstanceId, user3a), failure -> failure.assertException(CaseTeamError.class, "Role Manager is not allowed for user3 since user3 also has role Admin"));

        // Remove the Admin role, and show that the user still cannot be added because we can only have one manager
        CaseTeamMember user3b = new CaseTeamMember("user3", "Manager");
        testCase.assertStepFails(new PutTeamMember(admin, caseInstanceId, user3b), failure -> failure.assertException(CaseTeamError.class, "Role Manager is already assigned to another user"));

        // Remove the Manager role as well, and show that now the user can be added, though roleless
        CaseTeamMember user3c = new CaseTeamMember("user3");
        testCase.addStep(new PutTeamMember(admin, caseInstanceId, user3c), casePlan -> casePlan.print());

        // Fail to remove a member from the team that is not inside.
        testCase.assertStepFails(new RemoveTeamMember(admin, caseInstanceId, "I am not a user in the team"),
                failure -> failure.assertException(InvalidCommandException.class, "User I am not a user in the team cannot be removed from the case team"));

        // Successfully remove a member that is inside.
        testCase.addStep(new RemoveTeamMember(admin, caseInstanceId, "user1"), casePlan -> {
            casePlan.print();
            casePlan.getEvents().assertEvent("Expecting TeamMemberRemoved event for 'user1'", TeamMemberRemoved.class, e -> e.getUserId().equals("user1"));
        });

        testCase.runTest();
    }
}
