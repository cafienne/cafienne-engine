package org.cafienne.cmmn.test.expression;


import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.actorapi.event.plan.RepetitionRuleEvaluated;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemEvent;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.PublishedEventsAssertion;
import org.cafienne.cmmn.test.assertions.event.TaskOutputAssertion;
import org.cafienne.humantask.actorapi.command.CompleteHumanTask;
import org.junit.Test;

public class VariousSpelExpressions2 {
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/expression/spelexpressions2.xml");
    private final CaseDefinition definitionsWithDifferentPlanOrder = TestScript.getCaseDefinition("testdefinition/expression/spelexpressions2-different-plan-order.xml");

    private final String caseInstanceId = "SpelExpressionsTest";
    private final String input = "basic";
    private final String other = "other input";
    private final String defaultOutput = "My Output";
    private final String stopNowOutput = "stop now";
    private final String taskName = "HumanTask";
    private final String inputParameterName = "InputContent";
    private final ValueMap basicInput = new ValueMap(inputParameterName, input);
    private final ValueMap otherInput = new ValueMap(inputParameterName, other);
    private final ValueMap defaultTaskOutput = new ValueMap("Output", defaultOutput);
    private final ValueMap stopNowTaskOutput = new ValueMap("Output", stopNowOutput);
    private final TenantUser user = TestScript.getTestUser("user");

    @Test
    public void testHumanTaskExpressions() {
        TestScript testCase = new TestScript("expressions");
        testCase.addStep(new StartCase(user, caseInstanceId, definitions, basicInput, null), caseStarted -> {
            caseStarted.print();
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();

            // Now complete the HumanTask with the default output, and validate the task output filled event
            testCase.addStep(new CompleteHumanTask(user, caseInstanceId, taskId, defaultTaskOutput.cloneValueNode()), taskCompleted -> {
                taskCompleted.print();
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
                    toa.assertValue("TaskOutput", defaultOutput);
                    return true;
                });

                testCase.getEventListener().awaitPlanItemTransitioned("HumanTask",  e -> e.getTransition().equals(Transition.Complete));
            });
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneTerminationOnDifferentInput() {
        TestScript testCase = new TestScript("expressions");
        // Using "other" input should immediately make the Milestone occur, and also have the HumanTask end up terminated, because the stage is terminated
        testCase.addStep(new StartCase(user, caseInstanceId, definitions, otherInput, null), caseStarted -> {
            caseStarted.print();

            // Milestone must have occured, causing stage and task to be terminated;
            //  But they must also have been active
            testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active);
            testCase.getEventListener().awaitPlanItemState("Stage", State.Active);
            testCase.getEventListener().awaitPlanItemState("HumanTask", State.Terminated);
            testCase.getEventListener().awaitPlanItemState("Stage", State.Terminated);
            testCase.getEventListener().awaitPlanItemState("Milestone", State.Completed);

            PublishedEventsAssertion startCaseEvents = caseStarted.getEvents().filter(caseInstanceId);
            TestScript.debugMessage("Start case generated these events:\n" + startCaseEvents.enumerateEventsByType());
            int expectedNumberOfEvents = 32;
            if (startCaseEvents.getEvents().size() != expectedNumberOfEvents) {
                TestScript.debugMessage("Expected these events:\nStart case generated these events:\n" +
                        "CaseDefinitionApplied: 1\n" +
                        "TeamRoleFilled: 1\n" +
                        "CaseOwnerAdded: 1\n" +
                        "CaseFileEvent: 1\n" +
                        "PlanItemCreated: 4\n" +
                        "PlanItemTransitioned: 10\n" +
                        "RepetitionRuleEvaluated: 3\n" +
                        "RequiredRuleEvaluated: 3\n" +
                        "TaskInputFilled: 1\n" +
                        "HumanTaskActivated: 1\n" +
                        "HumanTaskAssigned: 1\n" +
                        "HumanTaskOwnerChanged: 1\n" +
                        "HumanTaskDueDateFilled: 1\n" +
                        "HumanTaskInputSaved: 1\n" +
                        "HumanTaskTerminated: 1\n" +
                        "CaseModified: 1\n");
            }
            caseStarted.getEvents().assertSize(expectedNumberOfEvents);
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneTerminationOnDifferentInput_DIFFERENT_PLAN_ORDER_XML_SHOULD_NOT_HAVE_DIFFERENT_BEHAVIOR() {
        // NOTE: THIS TEST HAS A DIFFERENT DEFINITION, BUT THE ONLY DIFFERENCE IS THAT IT HAS A DIFFERENT ORDER OF THE XML INSIDE THE CASE PLAN

        // See below old comments.
        // This now has finally been fixed. Order or XML no longer relevant! Tests should have same output.

        // Subsequently, this test has a DIFFERENT outcome than the test written directly above here.
        // One can consider this a bug ...
        //  The "cause" of the "bug" is that the lifecycle of a stage (the caseplan) is started, and within that lifecycle all child plan items
        //  are started. One by one. First create, then start; Next create, then start; But start leads to behavior.
        //  So an alternative would be that a stage, when activated, first creates all it's children, and only then starts all it's children.
        //  But this is quite a change ... Have to discuss first before changing it in the engine...

        TestScript testCase = new TestScript("expressions");
        // Using "other" input should immediately make the Milestone occur, and also have the HumanTask end up terminated, because the stage is terminated
        testCase.addStep(new StartCase(user, caseInstanceId, definitionsWithDifferentPlanOrder, otherInput, null), caseStarted -> {
            caseStarted.print();

            // Milestone must have occured, causing stage and task to be terminated;
            //  But they must also have been active
            testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active);
            testCase.getEventListener().awaitPlanItemState("Stage", State.Active);
            testCase.getEventListener().awaitPlanItemState("HumanTask", State.Terminated);
            testCase.getEventListener().awaitPlanItemState("Stage", State.Terminated);
            testCase.getEventListener().awaitPlanItemState("Milestone", State.Completed);

            PublishedEventsAssertion startCaseEvents = caseStarted.getEvents().filter(caseInstanceId);
            TestScript.debugMessage("Start case generated these events:\n" + startCaseEvents.enumerateEventsByType());
            int expectedNumberOfEvents = 32;
            if (startCaseEvents.getEvents().size() != expectedNumberOfEvents) {
                TestScript.debugMessage("Expected these events:\nStart case generated these events:\n" +
                        "CaseDefinitionApplied: 1\n" +
                        "TeamRoleFilled: 1\n" +
                        "CaseOwnerAdded: 1\n" +
                        "CaseFileEvent: 1\n" +
                        "PlanItemCreated: 4\n" +
                        "PlanItemTransitioned: 10\n" +
                        "RepetitionRuleEvaluated: 3\n" +
                        "RequiredRuleEvaluated: 3\n" +
                        "TaskInputFilled: 1\n" +
                        "HumanTaskActivated: 1\n" +
                        "HumanTaskAssigned: 1\n" +
                        "HumanTaskOwnerChanged: 1\n" +
                        "HumanTaskDueDateFilled: 1\n" +
                        "HumanTaskInputSaved: 1\n" +
                        "HumanTaskTerminated: 1\n" +
                        "CaseModified: 1\n");
            }
            caseStarted.getEvents().assertSize(expectedNumberOfEvents);
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneDrivenTerminationOnTaskInstanceLimit() {
        TestScript testCase = new TestScript("expressions");
        testCase.addStep(new StartCase(user, caseInstanceId, definitions, basicInput, null), caseStarted -> {
            caseStarted.print();

            // Now await the first HumanTask to become active, and then complete it, with the default task output;
            //  This ought to result in a new HumanTask, which we will also complete.
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();
            testCase.addStep(new CompleteHumanTask(user, caseInstanceId, taskId, defaultTaskOutput.cloneValueNode()), action -> {
                // Validate output again. Does not add too much value, as this is also done in the test above
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
                    toa.assertValue("TaskOutput", defaultOutput);
                    return true;
                });

                // Determine the id of the newly available HumanTask, so that we can complete that one too.
                String nextTaskId = testCase.getEventListener().awaitPlanItemTransitioned("HumanTask", e -> (!e.getPlanItemId().equals(taskId)) && e.getCurrentState().equals(State.Active)).getPlanItemId();

                // Print the case...
//                TestScript.debugMessage("Current case: " + new CaseAssertion(action));

                testCase.addStep(new CompleteHumanTask(user, caseInstanceId, nextTaskId, defaultTaskOutput.cloneValueNode()), result -> {
//                    TestScript.debugMessage("Current case: " + new CaseAssertion(result));
                    // Await completion of 'nextTaskId'
                    testCase.getEventListener().awaitPlanItemState(nextTaskId, State.Completed);
                    // There must be yet another HumanTask, but it must be in state terminated, and also it may not repeat
                    String lastTaskId = testCase.getEventListener().awaitPlanItemTransitioned("HumanTask", e ->
                            !(e.getPlanItemId().equals(taskId) || e.getPlanItemId().equals(nextTaskId)) && e.getCurrentState().equals(State.Terminated)).getPlanItemId();
                    // Last task must not repeat
                    testCase.getEventListener().awaitPlanItemEvent(lastTaskId, RepetitionRuleEvaluated.class, e -> !e.isRepeating());
                });
            });
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneDrivenTerminationOnTaskOutputContent() {
        TestScript testCase = new TestScript("expressions");
        testCase.addStep(new StartCase(user, caseInstanceId, definitions, basicInput, null), caseStarted -> {
            caseStarted.print();
            // Get the id of the first "HumanTask" in the case. It must be in state Active
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();
            // Now complete that task with the "stop now" output; this should make the milestone Occur, which must Terminate the Stage.
            testCase.addStep(new CompleteHumanTask(user, caseInstanceId, taskId, stopNowTaskOutput.cloneValueNode()), action -> {
                // Validate the output; it must be 'stop now'
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
                    toa.assertValue("TaskOutput", stopNowOutput);
                    return true;
                });

                // Assert that the Stage has become Terminated.
                testCase.getEventListener().awaitPlanItemEvent("Stage", PlanItemTransitioned.class, e -> e.getCurrentState().equals(State.Terminated));

                // Now fetch the next task id
                String nextTaskId = testCase.getEventListener().awaitPlanItemEvent("HumanTask", PlanItemEvent.class, e -> !e.getPlanItemId().equals(taskId)).getPlanItemId();

//                CaseAssertion casePlan = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + casePlan);


                testCase.assertStepFails(new CompleteHumanTask(user, caseInstanceId, nextTaskId, defaultTaskOutput.cloneValueNode()), failure -> {
                    failure.print();
                    // Last HumanTask must be terminated
                    testCase.getEventListener().awaitPlanItemState(nextTaskId, State.Terminated);
                });
            });
        });

        testCase.runTest();
    }
}
