package org.cafienne.cmmn.test.task;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.StringValue;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.FailureAssertion;
import org.cafienne.cmmn.test.assertions.HumanTaskAssertion;
import org.cafienne.humantask.akka.command.*;
import org.cafienne.humantask.instance.TaskState;
import org.junit.Test;

import java.time.Instant;

public class TestHumanTask {
    @Test
    public void testHumanTask() {
        String caseInstanceId = "HumanTaskTest";
        TestScript testCase = new TestScript("HumanTaskTest");

        CaseDefinition xml = TestScript.getCaseDefinition("testdefinition/task/testhumantask.xml");

        ValueMap inputs = new ValueMap();
        ValueMap taskInput = inputs.with("TaskInput");
        taskInput.putRaw("DueDate", "tomorrow");
        taskInput.putRaw("Assignee", "me, myself and I");
        ValueMap taskContent = taskInput.with("Content");
        taskContent.putRaw("Subject", "Decide on this topic");
        taskContent.putRaw("Decision", "Yet to be decided");

        TenantUser pete = TestScript.getTestUser("pete");
        TenantUser gimy = TestScript.getTestUser("gimy");

        testCase.addTestStep(new StartCase(pete, caseInstanceId, xml, inputs, null), act -> {
            CaseAssertion cp = new CaseAssertion(act);
            TestScript.debugMessage("Current case: " + cp);
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Available).getPlanItemId();
            TestScript.debugMessage("Task ID: " + taskId);

            ValueMap taskOutputDecisionCanceled = new ValueMap("Decision", "Cancel the order");
            ValueMap taskOutputDecisionApproved = new ValueMap("Decision", "Order Approved");

            /**
             * FillTaskDueDate - User should be able to set task due date using FillTaskDueDate command
             */
            Instant taskDueDate = Instant.now();
            testCase.addTestStep(new FillTaskDueDate(pete, caseInstanceId, taskId, taskDueDate), action -> {

//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);
//
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertDueDate(taskDueDate);
            });

            /**
             * SaveTaskOutput - User should not be able to save the task output for Unassigned task
             */
            testCase.addTestStep(new SaveTaskOutput(pete, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action ->
                    new FailureAssertion(action).assertException("Output can be saved only for Assigned or Delegated task"));

            /**
             * DelegateTask - Only Assigned task can be delegated
             */
            testCase.addTestStep(new DelegateTask(gimy, caseInstanceId, taskId, "pete"), action ->
                    new FailureAssertion(action).assertException("Only Assigned task can be delegated"));

            /**
             * CompleteTask - Only Assigned or Delegated task can be completed
             */
            testCase.addTestStep(new CompleteHumanTask(gimy, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action ->
                    new FailureAssertion(action).assertException("Only Assigned or Delegated task can be completed"));

            /**
             * ClaimTask - User should be able to claim the task
             */
            testCase.addTestStep(new ClaimTask(pete, caseInstanceId, taskId), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertAssignee("pete");
            });

            /**
             * ClaimTask - User should not be able to claim already Assigned task
             */
            testCase.addTestStep(new ClaimTask(pete, caseInstanceId, taskId), action ->
                    new FailureAssertion(action).assertException("Action can not be completed as the task (" + taskId + ") is in Assigned state"));

            /**
             * AssignTask - Only Unassigned task can be assigned to a user
             */
            testCase.addTestStep(new AssignTask(pete, caseInstanceId, taskId, "gimy"), action ->
                    new FailureAssertion(action).assertException("Action can not be completed as the task (" + taskId + ") is in Assigned state"));

            /**
             * ValidateTaskOutput - Only the current assignee should be able to validate task output
             */
            testCase.addTestStep(new ValidateTaskOutput(gimy, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action ->
                    new FailureAssertion(action).assertException("Only the current task assignee (pete) can validate output of task"));

            /**
             * SaveTaskOutput - Only the current assignee should be able to save task data
             */
            testCase.addTestStep(new SaveTaskOutput(gimy, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action ->
                    new FailureAssertion(action).assertException("Only the current task assignee (pete) can save the task"));

            /**
             * SaveTaskOutput - User should be able to save the task
             */
            testCase.addTestStep(new SaveTaskOutput(pete, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertTaskOutput(taskOutputDecisionCanceled);
            });

            /**
             * RevokeTask - Only the current assignee can revoke the task
             */
            testCase.addTestStep(new RevokeTask(gimy, caseInstanceId, taskId), action ->
                    new FailureAssertion(action).assertException("Only the current task assignee (pete) can revoke the task"));

            /**
             * RevokeTask - User should be able to revoke the task from Assigned state
             */
            testCase.addTestStep(new RevokeTask(pete, caseInstanceId, taskId), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertTaskState(TaskState.Unassigned);
            });

            /**
             * RevokeTask - Only Assigned or Delegated task can be revoked
             */
            testCase.addTestStep(new RevokeTask(gimy, caseInstanceId, taskId), action ->
                    new FailureAssertion(action).assertException("Only Assigned or Delegated task can be revoked"));

            /**
             * AssignTask - User should be able to assign the task to another user
             */
            testCase.addTestStep(new AssignTask(pete, caseInstanceId, taskId, "gimy"), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertAssignee("gimy");
            });

            /**
             * DelegateTask - Only the current task assignee can delegate the task to another user
             */
            testCase.addTestStep(new DelegateTask(pete, caseInstanceId, taskId, "pete"), action ->
                    new FailureAssertion(action).assertException("Only the current task assignee (gimy) can delegate the task"));

            /**
             * DelegateTask - User should be able to delegate the task
             */
            testCase.addTestStep(new DelegateTask(gimy, caseInstanceId, taskId, "pete"), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertOwner("gimy");
                taskAssertion.assertAssignee("pete");
            });

            /**
             * DelegateTask - Already delegated task can not be further delegated
             */
            testCase.addTestStep(new DelegateTask(pete, caseInstanceId, taskId, "pete"), action ->
                    new FailureAssertion(action).assertException("Action can not be completed as the task (" + taskId + ") is in Delegated state"));

            /**
             * RevokeTask - User should be able to revoke a task from Delegated state
             */
            testCase.addTestStep(new RevokeTask(pete, caseInstanceId, taskId), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertTaskState(TaskState.Assigned);
                taskAssertion.assertAssignee("gimy");
            });

            /**
             * CompleteTask - Only the current task assignee should be able to complete the task
             */
            testCase.addTestStep(new CompleteHumanTask(pete, caseInstanceId, taskId, taskOutputDecisionApproved.cloneValueNode()), action ->
                    new FailureAssertion(action).assertException("Only the current task assignee (gimy) can complete the task"));

            /**
             * CompleteTask - User should be able to complete the task
             */
            testCase.addTestStep(new CompleteHumanTask(gimy, caseInstanceId, taskId, taskOutputDecisionApproved.cloneValueNode()), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);

                testCase.getEventListener().awaitTaskOutputFilled("HumanTask", taskEvent -> {
                    ValueMap taskOutput = taskEvent.getTaskOutputParameters();
                    Value<?> decision = taskOutput.get("TaskOutputParameter");
                    if (decision == null || decision.equals(Value.NULL)) {
                        throw new AssertionError("Task misses output parameter 'TaskOutputParameter'");
                    }
                    if (decision instanceof StringValue) {
                        String value = ((StringValue) decision).getValue();
                        if (!value.equals("Order Approved")) {
                            throw new AssertionError("Task has invalid output. Expecting 'Order Approved', found " + value);
                        }
                    } else {
                        throw new AssertionError("Decision is not a string value, but a " + decision.getClass().getName());
                    }

                    return true;
                });

                taskAssertion.assertTaskCompleted();
            });
        });

        testCase.runTest();
    }
}
