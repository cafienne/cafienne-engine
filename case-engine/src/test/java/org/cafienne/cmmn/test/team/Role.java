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
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.TransitionDeniedException;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.FailureAssertion;
import org.cafienne.cmmn.user.CaseTeamError;
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

        testCase.addTestStep(new StartCase(anonymous, caseInstanceId, TestScript.getCaseDefinition("testdefinition/team/roles.xml"), null, caseTeam), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        // With Role admin
        testCase.addTestStep(new MakePlanItemTransition(admin, caseInstanceId, null, Transition.Occur, "UserEvent1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        // With Role admin
        testCase.addTestStep(new MakePlanItemTransition(admin, caseInstanceId, null, Transition.Complete, "Item1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        // With anonymous
        testCase.addTestStep(new MakeCaseTransition(anonymous, caseInstanceId, Transition.Suspend), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        testCase.addTestStep(new MakeCaseTransition(anonymous, caseInstanceId, Transition.Reactivate), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        testCase.addTestStep(new MakePlanItemTransition(employee, caseInstanceId, null, Transition.Complete, "Item1.1"), action ->
                new FailureAssertion(action).assertException(TransitionDeniedException.class, "You do not have the permission to perform the task"));

        // With admin user
        testCase.addTestStep(new MakePlanItemTransition(admin, caseInstanceId, null, Transition.Complete, "Item1.1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        testCase.addTestStep(new MakeCaseTransition(admin, caseInstanceId, Transition.Complete), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        testCase.runTest();
    }

    @Test
    public void testMutexAndSingletonRoles() {

        TestScript testCase = new TestScript(testName);

        // Start a case without a team;
        testCase.addTestStep(new StartCase(anonymous, caseInstanceId, TestScript.getCaseDefinition("testdefinition/team/roles.xml"), null, null), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        // Now create a team;
        CaseTeamMember user1 = new CaseTeamMember("user1", "Admin", "Employee");
        CaseTeamMember user2 = new CaseTeamMember("user2", "Manager");
        CaseTeam caseTeam = new CaseTeam(user1, user2);
        testCase.addTestStep(new SetCaseTeam(admin, caseInstanceId, caseTeam), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        // Fail to add a user with conflicting roles.
        CaseTeamMember user3a = new CaseTeamMember("user3", "Admin", "Manager");
        testCase.addTestStep(new PutTeamMember(admin, caseInstanceId, user3a), action ->
                new FailureAssertion(action).assertException(CaseTeamError.class, "Role Manager is not allowed for user3 since user3 also has role Admin"));

        // Remove the Admin role, and show that the user still cannot be added because we can only have one manager
        CaseTeamMember user3b = new CaseTeamMember("user3", "Manager");
        testCase.addTestStep(new PutTeamMember(admin, caseInstanceId, user3b), action ->
                new FailureAssertion(action).assertException(CaseTeamError.class, "Role Manager is already assigned to another user"));

        // Remove the Manager role as well, and show that now the user can be added, though roleless
        CaseTeamMember user3c = new CaseTeamMember("user3");
        testCase.addTestStep(new PutTeamMember(admin, caseInstanceId, user3c), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
        });

        // Fail to remove a member from the team that is not inside.
        testCase.addTestStep(new RemoveTeamMember(admin, caseInstanceId, "I am not a user in the team"), action ->
                new FailureAssertion(action).assertException(InvalidCommandException.class, "User I am not a user in the team cannot be removed from the case team"));

        // Successfully remove a member that is inside.
        testCase.addTestStep(new RemoveTeamMember(admin, caseInstanceId, "user1"), action -> {
            CaseAssertion casePlan = new CaseAssertion(action);
            TestScript.debugMessage(casePlan);
            // TODO: Add validation that the member is actually remove from the team.
        });

        testCase.runTest();
    }
}
