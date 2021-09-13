package org.cafienne.cmmn.test.task;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.HumanTaskAssertion;
import org.cafienne.humantask.actorapi.command.*;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.json.StringValue;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
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
        TenantUser tom = TestScript.getTestUser("tom");
        TenantUser notInTeam = TestScript.getTestUser("not-in-team");
        CaseTeam team = TestScript.getCaseTeam(pete, gimy, TestScript.getOwner(tom));

        testCase.addStep(new StartCase(pete, caseInstanceId, xml, inputs, team), caseStarted -> {
            caseStarted.print();
            String taskId = testCase.getEventListener().awaitPlanItemState("HumanTask", State.Available).getPlanItemId();
            TestScript.debugMessage("Task ID: " + taskId);

            String adminTaskId = testCase.getEventListener().awaitPlanItemState("AdminTask", State.Available).getPlanItemId();
            TestScript.debugMessage("Admin task ID: " + taskId);

            ValueMap taskOutputDecisionCanceled = new ValueMap("Decision", "Cancel the order");
            ValueMap taskOutputDecisionApproved = new ValueMap("Decision", "Order Approved");

            /**
             * FillTaskDueDate - User should be able to set task due date using FillTaskDueDate command
             */
            Instant taskDueDate = Instant.now();
            testCase.addStep(new FillTaskDueDate(tom, caseInstanceId, taskId, taskDueDate), action -> {

//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);
//
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertDueDate(taskDueDate);
            });

            /**
             * SaveTaskOutput - pete should not be able to save the task output for Unassigned AdminTask (lack of appropriate role)
             */
            testCase.assertStepFails(new SaveTaskOutput(pete, caseInstanceId, adminTaskId, taskOutputDecisionCanceled.cloneValueNode()),
                    failure -> failure.assertException("You do not have permission to perform this operation"));

            /**
             * CompleteTask - pete should not be able to complete AdminTask (lack of appropriate role)
             */
            testCase.assertStepFails(new CompleteHumanTask(pete, caseInstanceId, adminTaskId, taskOutputDecisionCanceled.cloneValueNode()),
                    failure -> failure.assertException("You do not have permission to perform this operation"));

            /**
             * SaveTaskOutput - Although tom doesn't have appropriate role, can save output for Unassigned AdminTask (as tom is case team owner)
             */
            testCase.addStep(new SaveTaskOutput(tom, caseInstanceId, adminTaskId, taskOutputDecisionCanceled.cloneValueNode()), action -> {
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertTaskOutput(taskOutputDecisionCanceled);
            });

            /**
             * SaveTaskOutput - User should be able to save the task output for Unassigned task
             */
            testCase.addStep(new SaveTaskOutput(pete, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action -> {
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertTaskOutput(taskOutputDecisionCanceled);
            });

            /**
             * DelegateTask - Only Assigned task can be delegated
             */
            testCase.assertStepFails(new DelegateTask(gimy, caseInstanceId, taskId, pete),
                    failure -> failure.assertException("Cannot be done because the task is in Unassigned state, but should be in any of [Assigned] state"));

            /**
             * CompleteTask - Outside of case team members cannot take actions on task
             */
            testCase.assertStepFails(new CompleteHumanTask(notInTeam, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()),
                    failure -> failure.assertException("User not-in-team is not part of the case team"));

            /**
             * ClaimTask - User should be able to claim the task
             */
            testCase.addStep(new ClaimTask(pete, caseInstanceId, taskId), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertAssignee("pete");
            });

            /**
             * ClaimTask - User should not be able to claim already Assigned task
             */
            testCase.assertStepFails(new ClaimTask(pete, caseInstanceId, taskId),
                    failure -> failure.assertException("Cannot be done because the task is in Assigned state, but should be in any of [Unassigned] state"));

            /**
             * AssignTask - Only Unassigned task can be assigned to a user
             */
            testCase.assertStepFails(new AssignTask(pete, caseInstanceId, taskId, gimy),
                    failure -> failure.assertException("You must be case owner to perform this operation"));

            /**
             * ValidateTaskOutput - Only the current assignee should be able to validate task output
             */
            testCase.assertStepFails(new ValidateTaskOutput(gimy, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()),
                    failure -> failure.assertException("You do not have permission to perform this operation"));

            /**
             * SaveTaskOutput - Only the current assignee should be able to save task data
             */
            testCase.assertStepFails(new SaveTaskOutput(gimy, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()),
                    failure -> failure.assertException("You do not have permission to perform this operation"));

            /**
             * SaveTaskOutput - User should be able to save the task
             */
            testCase.addStep(new SaveTaskOutput(pete, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertTaskOutput(taskOutputDecisionCanceled);
            });

            /**
             * RevokeTask - Only the current assignee can revoke the task
             */
            testCase.assertStepFails(new RevokeTask(gimy, caseInstanceId, taskId),
                    failure -> failure.assertException("You do not have permission to perform this operation"));

            /**
             * RevokeTask - User should be able to revoke the task from Assigned state
             */
            testCase.addStep(new RevokeTask(pete, caseInstanceId, taskId), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertTaskState(TaskState.Unassigned);
            });

            /**
             * RevokeTask - Only Assigned or Delegated task can be revoked
             */
            testCase.assertStepFails(new RevokeTask(gimy, caseInstanceId, taskId),
                    failure -> failure.assertException("Cannot be done because the task is in Unassigned state, but should be in any of [Assigned, Delegated] state"));

            /**
             * AssignTask - User should be able to assign the task to another user
             */
            testCase.addStep(new AssignTask(tom, caseInstanceId, taskId, gimy), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertAssignee("gimy");
            });

            /**
             * DelegateTask - Only the current task assignee can delegate the task to another user
             */
            testCase.assertStepFails(new DelegateTask(pete, caseInstanceId, taskId, pete),
                    failure -> failure.assertException("You do not have permission to perform this operation"));

            /**
             * DelegateTask - User should be able to delegate the task
             */
            testCase.addStep(new DelegateTask(gimy, caseInstanceId, taskId, pete), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertOwner("gimy");
                taskAssertion.assertAssignee("pete");
            });

            /**
             * DelegateTask - Already delegated task can not be further delegated
             */
            testCase.assertStepFails(new DelegateTask(pete, caseInstanceId, taskId, pete),
                    failure -> failure.assertException("Cannot be done because the task is in Delegated state, but should be in any of [Assigned] state"));

            /**
             * RevokeTask - User should be able to revoke a task from Delegated state
             */
            testCase.addStep(new RevokeTask(pete, caseInstanceId, taskId), action -> {
//                CaseAssertion taskAssertion = new CaseAssertion(action);
//                TestScript.debugMessage("Current case: " + taskAssertion);

                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertTaskState(TaskState.Assigned);
                taskAssertion.assertAssignee("gimy");
            });

            /**
             * CompleteTask - Only the current task assignee should be able to complete the task
             */
            testCase.assertStepFails(new CompleteHumanTask(pete, caseInstanceId, taskId, taskOutputDecisionApproved.cloneValueNode()),
                    failure -> failure.assertException("You do not have permission to perform this operation"));

            /**
             * CompleteTask - User should be able to complete the task
             */
            testCase.addStep(new CompleteHumanTask(gimy, caseInstanceId, taskId, taskOutputDecisionApproved.cloneValueNode()), action -> {
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
