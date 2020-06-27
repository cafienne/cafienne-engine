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
import org.cafienne.cmmn.akka.event.team.TeamRoleCleared;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.TransitionDeniedException;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.test.TestScript;
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
        CaseTeam caseTeam = TestScript.getCaseTeam(TestScript.getOwner(anonymous), admin, employee);

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

        // Try to set a team with 2 captains should fail.
        TenantUser captain1 = TestScript.getTestUser("c1", "Manager");
        TenantUser captain2 = TestScript.getTestUser("c2", "Manager");
        CaseTeam teamWithTwoCaptainsAlwaysFails = TestScript.getCaseTeam(TestScript.getOwner(captain1), TestScript.getOwner(captain2));
        testCase.assertStepFails(new SetCaseTeam(anonymous, caseInstanceId, teamWithTwoCaptainsAlwaysFails), failure -> failure.assertException(CaseTeamError.class, "Role 'Manager' cannot be assigned to more than one team member"));

        // Try to set a team with a corrupt member.
        TenantUser corruptOfficer = TestScript.getTestUser("c1", "Manager", "Admin");
        CaseTeam corruptedTeam = TestScript.getCaseTeam(TestScript.getOwner(corruptOfficer));
        testCase.assertStepFails(new SetCaseTeam(anonymous, caseInstanceId, corruptedTeam), failure -> failure.assertException(CaseTeamError.class, "A team member cannot have both roles 'Admin' and 'Manager'"));

        // Now create a valid team;
        TenantUser user1 = TestScript.getTestUser("user1", "Admin", "Employee");
        TenantUser user2 = TestScript.getTestUser("user2", "Manager");
        CaseTeam caseTeam = TestScript.getCaseTeam(TestScript.getOwner(user1), user2);

        testCase.addStep(new SetCaseTeam(anonymous, caseInstanceId, caseTeam), casePlan -> casePlan.print());

        // Fail to add a user with conflicting roles.
        CaseTeamMember user3a = TestScript.getMember(TestScript.getTestUser("user3", "Admin", "Manager"));
        testCase.assertStepFails(new PutTeamMember(user1, caseInstanceId, user3a), failure -> failure.assertException(CaseTeamError.class, "Role Admin is not allowed for user 'user3' since this member also has role Manager"));

        // Remove the Admin role, and show that the user still cannot be added because we can only have one manager
        CaseTeamMember user3b = TestScript.getMember(TestScript.getTestUser("user3", "Manager"));
        testCase.assertStepFails(new PutTeamMember(user1, caseInstanceId, user3b), failure -> failure.assertException(CaseTeamError.class, "Role Manager is already assigned to another user"));

        // Remove the Manager role as well, and show that now the user can be added, though roleless
        CaseTeamMember user3c = TestScript.getMember(TestScript.getTestUser("user3"));
        testCase.addStep(new PutTeamMember(user1, caseInstanceId, user3c), casePlan -> casePlan.print());

        // Fail to remove a member from the team that is not inside.
        testCase.assertStepFails(new RemoveTeamMember(user1, caseInstanceId, new MemberKey("I am not a user in the team", "user")),
                failure -> failure.assertException(InvalidCommandException.class, "The case team does not have a member with id user 'I am not a user in the team'"));

        // Successfully remove a member that is inside.
        testCase.addStep(new RemoveTeamMember(user1, caseInstanceId, new MemberKey("user2", "user")), casePlan -> {
            casePlan.print();
            TestScript.debugMessage("Start case generated these events:\n" + casePlan.getEvents().enumerateEventsByType());
            casePlan.getEvents().assertEvent("Expecting TeamRoleCleared event for 'user2'", TeamRoleCleared.class, e -> e.key.id().equals("user2") && e.isMemberItself());
        });

        testCase.runTest();
    }
}
