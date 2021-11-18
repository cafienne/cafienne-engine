package org.cafienne.cmmn.test.task;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.humantask.actorapi.command.CompleteHumanTask;
import org.cafienne.json.ValueMap;
import org.junit.Test;

public class TestTaskOutputParameters {

    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/task/requiredtaskparameters.xml");
    private final TenantUser testUser = TestScript.getTestUser("user");
    private final ValueMap inputs = new ValueMap();
    private final ValueMap emptyTaskOutput = new ValueMap();
    private final ValueMap invalidTaskOutput = new ValueMap("Non-Result", "This is an invalid output parameter");
    private final ValueMap properTaskOutput = new ValueMap("Result", new ValueMap("Outcome", 6));

    @Test
    public void testTaskOutputParameters() {
        String caseInstanceId = "testTaskOutputParameters";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions, inputs);
        testCase.addStep(startCase, act -> {
            PlanItemTransitioned taskWithRequiredOutput = testCase.getEventListener().awaitPlanItemState("TaskRequiredOutput", State.Active);
            String requiredTaskId = taskWithRequiredOutput.getPlanItemId();
            testCase.assertStepFails(new CompleteHumanTask(testUser, caseInstanceId, requiredTaskId, emptyTaskOutput.cloneValueNode()), failure1 -> {
                failure1.assertException("Task output parameter Result does not have a value, but that is required in order to complete the task");
                // Now test with invalid output
                testCase.insertStepFails(new CompleteHumanTask(testUser, caseInstanceId, requiredTaskId, invalidTaskOutput.cloneValueNode()), failure2 -> {
                    failure2.assertException("Task output parameter Result does not have a value, but that is required in order to complete the task");

                    // Now test the same with proper output
                    testCase.insertStep(new CompleteHumanTask(testUser, caseInstanceId, requiredTaskId, properTaskOutput.cloneValueNode()), response -> {
                        testCase.getEventListener().awaitPlanItemState(requiredTaskId, State.Completed);
                    });
                });
            });
        });
        testCase.runTest();
    }

    @Test
    public void testTaskOutputParametersWithCaseFileBinding() {
        // This case tests the validation of a task that has output and binding into the case file
        String caseInstanceId = "testTaskOutputParametersWithCaseFileBinding";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions, inputs);
        testCase.addStep(startCase, act -> {
            PlanItemTransitioned taskRequiredOutputWithBinding = testCase.getEventListener().awaitPlanItemState("TaskRequiredOutputWithBinding", State.Active);
            String requiredTaskWithBindingId = taskRequiredOutputWithBinding.getPlanItemId();
            testCase.assertStepFails(new CompleteHumanTask(testUser, caseInstanceId, requiredTaskWithBindingId, emptyTaskOutput.cloneValueNode()), failure1 -> {
                failure1.assertException("Task output parameter Result does not have a value, but that is required in order to complete the task");

                // Now test with invalid output
                testCase.insertStepFails(new CompleteHumanTask(testUser, caseInstanceId, requiredTaskWithBindingId, invalidTaskOutput.cloneValueNode()), failure2 -> {
                    failure2.assertException("Task output parameter Result does not have a value, but that is required in order to complete the task");
                    // Now test the same with proper output
                    testCase.insertStep(new CompleteHumanTask(testUser, caseInstanceId, requiredTaskWithBindingId, properTaskOutput.cloneValueNode()), response -> {
                        testCase.getEventListener().awaitPlanItemState(requiredTaskWithBindingId, State.Completed);

                        // TTD - test step below should test on whole set of events; requires refactoring of / addition to whole structure?

                        // "Outcome" inside "Root" must have value 6 (2 * 3)
                        response.assertCaseFile().awaitCaseFileEvent(new Path("Root"), e -> e.getValue().equals(new ValueMap("Outcome", 6)));
                    });
                });
            });
        });

        testCase.runTest();
    }

    @Test
    public void testTaskWithoutRequiredOutput() {
        // Test the one that task can be completed without output (below the same is tested with invalid output)
        String caseInstanceId = "testTaskWithoutRequiredOutput";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions, inputs);
        testCase.addStep(startCase, act -> {
            PlanItemTransitioned taskWithOutputNotRequired = testCase.getEventListener().awaitPlanItemState("TaskWithOutputNotRequired", State.Active);
            testCase.addStep(new CompleteHumanTask(testUser, caseInstanceId, taskWithOutputNotRequired.getPlanItemId(), emptyTaskOutput), response -> {
                testCase.getEventListener().awaitPlanItemState(taskWithOutputNotRequired.getPlanItemId(), State.Completed);
            });
        });

        testCase.runTest();
    }

    @Test
    public void testTaskWithoutRequiredOutput2() {
        // Test the one that task can be completed with invalid output
        String caseInstanceId = "testTaskWithoutRequiredOutput2";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions, inputs);
        testCase.addStep(startCase, act -> {
            PlanItemTransitioned taskWithOutputNotRequired = testCase.getEventListener().awaitPlanItemState("TaskWithOutputNotRequired", State.Active);
            testCase.addStep(new CompleteHumanTask(testUser, caseInstanceId, taskWithOutputNotRequired.getPlanItemId(), invalidTaskOutput), response -> {
                testCase.getEventListener().awaitPlanItemState(taskWithOutputNotRequired.getPlanItemId(), State.Completed);
            });
        });

        testCase.runTest();
    }


    @Test
    public void testTaskWithoutRequiredOutputButWithCaseFileBinding() {
        // Here we test a task that does not check for a mandatory output parameter, but it has a spel expression in the binding, which fails on invalid output.
        String caseInstanceId = "testTaskWithoutRequiredOutputButWithCaseFileBinding";
        TestScript testCase = new TestScript(caseInstanceId);

        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions, inputs);
        testCase.addStep(startCase, act -> {
            PlanItemTransitioned taskWithoutOutputWithBinding = testCase.getEventListener().awaitPlanItemState("TaskWithOutputNotRequiredAndBinding", State.Active);
            String taskWithoutOutputWithBindingId = taskWithoutOutputWithBinding.getPlanItemId();
            testCase.assertStepFails(new CompleteHumanTask(testUser, caseInstanceId, taskWithoutOutputWithBindingId, emptyTaskOutput.cloneValueNode()), failure1 -> {
                failure1.print();
                failure1.assertException(InvalidExpressionException.class, "Could not evaluate");
                // Now test the same with proper output
                testCase.insertStep(new CompleteHumanTask(testUser, caseInstanceId, taskWithoutOutputWithBindingId, properTaskOutput.cloneValueNode()), response -> {
                    testCase.getEventListener().awaitPlanItemState(taskWithoutOutputWithBindingId, State.Completed);
                });
            });
        });

        testCase.runTest();
    }
}
