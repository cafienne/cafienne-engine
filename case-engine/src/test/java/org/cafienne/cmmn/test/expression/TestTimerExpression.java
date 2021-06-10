package org.cafienne.cmmn.test.expression;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.cmmn.test.PingCommand;
import org.cafienne.cmmn.test.TestScript;
import org.junit.Test;

public class TestTimerExpression {

    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/timerexpression.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testTimerExpression() {
        String caseInstanceId = "Timer";
        TestScript testCase = new TestScript(caseInstanceId);
        String period = "PT3S";
        ValueMap timerInput = new ValueMap("timer", new ValueMap("period", period));

        // Case contains a timer that runs after 3 seconds; it then starts a task.
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, timerInput, null);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Suspending and resuming is a means to validate that the Case scheduler actually cleans up the jobs and keeps the registration proper.
        //  Note: to validate that logic requires enabling of additional logging in the case scheduler and then checking in the debugger.
        //        or to run a code coverage tool and see we're touching that code.
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "AfterPeriod", Transition.Suspend));

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "AfterPeriod", Transition.Resume));

        // second test step should lead to recovery
        testCase.assertStepFails(new MakePlanItemTransition(testUser, caseInstanceId, "simplehumantask", Transition.Complete));

        // Waiting 1 second should not have changed anything; timer is still running
        testCase.addStep(new PingCommand(testUser, caseInstanceId, 1000), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Resume, State.Available, State.Suspended);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Waiting 5 seconds should have triggered the timer and the task should now be active
        testCase.addStep(new PingCommand(testUser, caseInstanceId, 5000), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Occur, State.Completed, State.Available);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Start, State.Active, State.Available);
        });

        testCase.runTest();
    }

    @Test
    public void testTimerExpressionSuspendAndCrashAndResume() {
        String caseInstanceId = "Timer";
        TestScript testCase = new TestScript(caseInstanceId);
        String period = "PT3S";
        ValueMap timerInput = new ValueMap("timer", new ValueMap("period", period));

        // Case contains a timer that runs after 3 seconds; it then starts a task.
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, timerInput, null);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Suspending and resuming is a means to validate that the Case scheduler actually cleans up the jobs and keeps the registration proper.
        //  Note: to validate that logic requires enabling of additional logging in the case scheduler and then checking in the debugger.
        //        or to run a code coverage tool and see we're touching that code.
        //  Also, this test case was added because it shows that recovery does not take the Suspend into account, and therefore after recovery still 2 timers are set...
        //   Second timer does not do any state changes to the case though.
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "AfterPeriod", Transition.Suspend),
                casePlan -> casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Suspend, State.Suspended, State.Available));

        // Waiting 1 second should not have changed anything; timer is still running
        testCase.addStep(new PingCommand(testUser, caseInstanceId, 1000), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Suspend, State.Suspended, State.Available);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });


        // This step leads to failure and recovery. It should suspend the schedule
        testCase.assertStepFails(new MakePlanItemTransition(testUser, caseInstanceId, "simplehumantask", Transition.Complete));

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "AfterPeriod", Transition.Resume), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Resume, State.Available, State.Suspended);
            TestScript.debugMessage("CasePLan after resume: \n\n" + casePlan + "\n\n\n");
        });

        // Waiting 5 seconds should have triggered the timer and the task should now be active
        testCase.addStep(new PingCommand(testUser, caseInstanceId, 5000), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Occur, State.Completed, State.Available);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Start, State.Active, State.Available);
        });

        testCase.runTest();
    }
}
