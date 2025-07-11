package org.cafienne.engine.cmmn.test.expression;

import org.cafienne.engine.cmmn.actorapi.command.StartCase;
import org.cafienne.engine.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.engine.cmmn.definition.CaseDefinition;
import org.cafienne.engine.cmmn.instance.State;
import org.cafienne.engine.cmmn.instance.Transition;
import org.cafienne.engine.cmmn.test.TestScript;
import org.cafienne.json.ValueMap;
import org.junit.Test;

import static org.cafienne.engine.cmmn.test.TestScript.*;

public class TestTimerExpression {

    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/timerexpression.xml");

    @Test
    public void testTimerExpressionSuspendResumeWithTermination() {
        String caseInstanceId = "Timer";
        TestScript testCase = new TestScript(caseInstanceId);
        String period = "PT2S";
        ValueMap timerInput = new ValueMap("timer", new ValueMap("period", period));

        // Case contains a timer that runs after 3 seconds; it then starts a task.
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, timerInput);
        testCase.addStep(startCase, casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Suspending and resuming is a means to validate that the Case scheduler actually cleans up the jobs and keeps the registration proper.
        //  Note: to validate that logic requires enabling of additional logging in the case scheduler and then checking in the debugger.
        //        or to run a code coverage tool and see we're touching that code.
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "AfterPeriod", Transition.Suspend));

        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "AfterPeriod", Transition.Resume));

        // Now force the case to be removed from memory. This should lead to recovery when the timer goes off.
        testCase.addStep(createTerminationCommand(testUser, caseInstanceId));

        // Waiting 3 seconds should have triggered the timer and the task should now be active
        testCase.addStep(createPingCommand(testUser, caseInstanceId, 3000), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Occur, State.Completed, State.Available);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Start, State.Active, State.Available);
        });

        testCase.runTest();
    }

    @Test
    public void testTimerExpressionSuspendResumeWithRecovery() {
        String caseInstanceId = "Timer";
        TestScript testCase = new TestScript(caseInstanceId);
        String period = "PT2S";
        ValueMap timerInput = new ValueMap("timer", new ValueMap("period", period));

        // Case contains a timer that runs after 3 seconds; it then starts a task.
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, timerInput);
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
        testCase.addStep(createPingCommand(testUser, caseInstanceId, 1000), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Suspend, State.Suspended, State.Available);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        // Resume the timer, then force recovery on the case should
        testCase.addStep(new MakePlanItemTransition(testUser, caseInstanceId, "AfterPeriod", Transition.Resume), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Resume, State.Available, State.Suspended);
            TestScript.debugMessage("CasePLan after resume: \n\n" + casePlan + "\n\n\n");
        });

        // Now force the case to be removed from memory and then recovered.
        //  The timer should still be pending.
        testCase.addStep(createRecoveryCommand(testUser, caseInstanceId), casePlan -> {
            TestScript.debugMessage("Recovered case instance: " + casePlan);
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Resume, State.Available, State.Suspended);
        });

        // Waiting 5 seconds should have triggered the timer and the task should now be active
        testCase.addStep(createPingCommand(testUser, caseInstanceId, 2000), casePlan -> {
            casePlan.assertPlanItem("AfterPeriod").assertLastTransition(Transition.Occur, State.Completed, State.Available);
            casePlan.assertPlanItem("Task1").assertLastTransition(Transition.Start, State.Active, State.Available);
        });

        testCase.runTest();
    }
}
