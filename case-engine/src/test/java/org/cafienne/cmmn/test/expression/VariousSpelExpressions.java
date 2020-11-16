package org.cafienne.cmmn.test.expression;


import org.cafienne.akka.actor.serialization.json.LongValue;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.akka.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.akka.event.plan.RepetitionRuleEvaluated;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.akka.event.plan.PlanItemEvent;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.event.TaskOutputAssertion;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.humantask.akka.command.CompleteHumanTask;
import org.junit.Test;

public class VariousSpelExpressions {
    private final CaseDefinition xml = TestScript.getCaseDefinition("testdefinition/expression/spelexpressions.xml");

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
    private final ValueMap specialTaskOutput = new ValueMap("SpecialOutput", new ValueMap("Multi", new int[]{1, 2, 3, 4}));
    private final TenantUser user = TestScript.getTestUser("user");

    @Test
    public void testHumanTaskExpressions() {
        TestScript testCase = new TestScript("expressions");
        testCase.addStep(new StartCase(user, caseInstanceId, xml, basicInput, null), caseStarted -> {
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
                    Value v = toa.getValue("MultiOutput");
//                    System.out.println("Value of special output: " + v);
                    return true;
                });

                testCase.getEventListener().awaitPlanItemEvent("HumanTask", PlanItemTransitioned.class, e -> e.getTransition().equals(Transition.Complete));
            });
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneTerminationOnMultipleMulti() {
        TestScript testCase = new TestScript("expressions");
        // Using "other" input should immediately make the Milestone occur, and also have the HumanTask end up terminated, because the stage is terminated
        testCase.addStep(new StartCase(user, caseInstanceId, xml, null, null), caseStarted -> {
            caseStarted.print();
            // Milestone must have occured, causing stage and task to be terminated
            testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active);
        });

        testCase.addStep(new CreateCaseFileItem(user, caseInstanceId, new Path("SpecialOutput/Multi"), new LongValue(1)), result -> {
            result.assertPlanItems("HumanTask").assertSize(1).assertStates(State.Active);
            result.assertPlanItems("Milestone").assertSize(1).assertStates(State.Available);
        });
        testCase.addStep(new CreateCaseFileItem(user, caseInstanceId, new Path("SpecialOutput/Multi"), new LongValue(2)), result -> {
            result.assertPlanItems("HumanTask").assertSize(1).assertStates(State.Active);
            result.assertPlanItems("Milestone").assertSize(1).assertStates(State.Available);
        });
        testCase.addStep(new CreateCaseFileItem(user, caseInstanceId, new Path("SpecialOutput/Multi"), new LongValue(3)), result -> {
            result.assertPlanItems("HumanTask").assertSize(1).assertStates(State.Active);
            result.assertPlanItems("Milestone").assertSize(1).assertStates(State.Available);
        });
        testCase.addStep(new CreateCaseFileItem(user, caseInstanceId, new Path("SpecialOutput/Multi"), new LongValue(4)), result -> {
            result.assertPlanItems("HumanTask").assertSize(2).assertStates(State.Terminated, State.Active);
            result.assertPlanItems("Milestone").assertSize(1).assertStates(State.Completed);
        });
        testCase.addStep(new CreateCaseFileItem(user, caseInstanceId, new Path("SpecialOutput/Multi"), new LongValue(5)), result -> {
            result.assertPlanItems("HumanTask").assertSize(3).assertStates(State.Terminated, State.Active);
            result.assertPlanItems("Milestone").assertSize(2).assertStates(State.Completed);
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneTerminationOnDifferentInput() {
        TestScript testCase = new TestScript("expressions");
        // Using "other" input should immediately make the Milestone occur, and also have the HumanTask end up terminated, because the stage is terminated
        testCase.addStep(new StartCase(user, caseInstanceId, xml, otherInput, null), caseStarted -> {
            caseStarted.print();
            // Milestone must have occured, causing stage and task to be terminated
            testCase.getEventListener().awaitPlanItemState("HumanTask", State.Terminated);
            testCase.getEventListener().awaitPlanItemState("Milestone", State.Completed);
        });

        testCase.runTest();
    }

    @Test
    public void testMilestoneDrivenTerminationOnTaskInstanceLimit() {
        TestScript testCase = new TestScript("expressions");
        testCase.addStep(new StartCase(user, caseInstanceId, xml, basicInput, null), casePlan -> {
            casePlan.print();

            // Now await the first HumanTask to become active, and then complete it, with the default task output;
            //  This ought to result in a new HumanTask, which we will also complete.
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();

            testCase.addStep(new CompleteHumanTask(user, caseInstanceId, taskId, defaultTaskOutput.cloneValueNode()), case2 -> {
                // Validate output again. Does not add too much value, as this is also done in the test above
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
                    toa.assertValue("TaskOutput", defaultOutput);
                    return true;
                });

                // Determine the id of the newly available HumanTask, so that we can complete that one too.
                String nextTaskId = testCase.getEventListener().awaitPlanItemTransitioned("HumanTask", e ->
                        !e.getPlanItemId().equals(taskId) && e.getCurrentState().equals(State.Active)).getPlanItemId();

                // Print the case...
//                case2.print();

                testCase.addStep(new CompleteHumanTask(user, caseInstanceId, nextTaskId, defaultTaskOutput.cloneValueNode()), case3 -> {
//                  case3.print();
                    // Await completion of 'nextTaskId'
                    testCase.getEventListener().awaitPlanItemState(nextTaskId, State.Completed);
                    // There must be yet another HumanTask, but it must be in state terminated, and also it may not repeat
                    String lastTaskId = testCase.getEventListener().awaitPlanItemTransitioned("HumanTask",
                            e -> (!(e.getPlanItemId().equals(taskId) || e.getPlanItemId().equals(nextTaskId)) && e.getCurrentState().equals(State.Terminated))).getPlanItemId();
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
        testCase.addStep(new StartCase(user, caseInstanceId, xml, basicInput, null), case1 -> {
            case1.print();
            // Get the id of the first "HumanTask" in the case. It must be in state Active
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();
            // Now complete that task with the "stop now" output; this should make the milestone Occur, which must Terminate the Stage.
            testCase.addStep(new CompleteHumanTask(user, caseInstanceId, taskId, stopNowTaskOutput.cloneValueNode()), case2 -> {
                // Validate the output; it must be 'stop now'
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
                    toa.assertValue("TaskOutput", stopNowOutput);
                    return true;
                });

                // Now fetch the next task id
                String nextTaskId = testCase.getEventListener().awaitPlanItemTransitioned("HumanTask", e -> !e.getPlanItemId().equals(taskId) && e.getCurrentState().equals(State.Active)).getPlanItemId();

//                case2.print();

                testCase.assertStepFails(new CompleteHumanTask(user, caseInstanceId, nextTaskId, defaultTaskOutput.cloneValueNode()), failure -> {
                    failure.print();
                    // Last HumanTask must be terminated
                    testCase.getEventListener().awaitPlanItemState(nextTaskId, State.Terminated);
                });
            });
        });

        testCase.runTest();
    }


    @Test
    public void testMilestoneDrivenTerminationOnTaskMultiOutputContent() {
        TestScript testCase = new TestScript("expressions");
        testCase.addStep(new StartCase(user, caseInstanceId, xml, basicInput, null), casePlan -> {
            casePlan.print();
            // Get the id of the first "HumanTask" in the case. It must be in state Active
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Active).getPlanItemId();
            // Now complete that task with the "stop now" output; this should make the milestone Occur, which must Terminate the Stage.
            testCase.addStep(new CompleteHumanTask(user, caseInstanceId, taskId, specialTaskOutput.cloneValueNode()), case2 -> {
                // Validate the output; it must be 'stop now'
                testCase.getEventListener().awaitTaskOutputFilled(taskName, taskEvent -> {
                    TaskOutputAssertion toa = new TaskOutputAssertion(taskEvent);
                    toa.assertValue("CaseID", caseInstanceId);
                    toa.assertValue("TaskName", taskName);
//                    System.out.println("Value of special output: " + toa.getValue("MultiOutput"));
                    return true;
                });

                // Now fetch the next task id
                String nextTaskId = testCase.getEventListener().awaitPlanItemEvent("HumanTask", PlanItemEvent.class, e -> !e.getPlanItemId().equals(taskId)).getPlanItemId();

//                case2.print();

                testCase.addStep(new CompleteHumanTask(user, caseInstanceId, nextTaskId, defaultTaskOutput.cloneValueNode()), case3 -> {
//                    case3.print();
                    // Last HumanTask must be terminated
                    testCase.getEventListener().awaitPlanItemState(nextTaskId, State.Completed);
                });
            });
        });

        testCase.runTest();
    }
}
