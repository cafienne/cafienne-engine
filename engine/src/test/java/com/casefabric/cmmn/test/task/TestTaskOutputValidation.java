package com.casefabric.cmmn.test.task;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.actorapi.command.team.CaseTeam;
import com.casefabric.cmmn.actorapi.event.plan.task.TaskOutputFilled;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.cmmn.test.TestUser;
import com.casefabric.cmmn.test.assertions.HumanTaskAssertion;
import com.casefabric.humantask.actorapi.command.ClaimTask;
import com.casefabric.humantask.actorapi.command.CompleteHumanTask;
import com.casefabric.humantask.actorapi.command.SaveTaskOutput;
import com.casefabric.humantask.actorapi.command.ValidateTaskOutput;
import com.casefabric.humantask.actorapi.event.HumanTaskOutputSaved;
import com.casefabric.json.*;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.casefabric.cmmn.test.TestScript.*;

public class TestTaskOutputValidation {
    private final ValueMap taskOutputFailingValidation = new ValueMap("Decision", "KILLSWITCH");
    private final ValueMap taskOutputInvalidDecision = new ValueMap("Decision", "afd;lsajfdba");

    private final ValueMap taskOutputDecisionCanceled = new ValueMap("Decision", "Cancel the order");
    private final ValueMap taskOutputDecisionApproved = new ValueMap("Decision", "Order Approved");

    private final ValueMap validDecisionResponse = new ValueMap(); // Valid task output means there must be empty JSON responded
    private final ValueMap invalidDecisionResponse = new ValueMap("Status", "NOK", "details", "Field 'decision' has an improper value");


    private final TestUser pete = createTestUser("pete");
    private final TestUser gimy = createTestUser("gimy");
    private final TestUser tom = createTestUser("tom");


    private final int port = 17382;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    @Test
    public void testTaskOutputValidation() {
        startWireMocks();

        String caseInstanceId = "OutputValidationTest";
        TestScript testCase = new TestScript(caseInstanceId);

        CaseDefinition xml = loadCaseDefinition("testdefinition/task/taskoutputvalidation.xml");

        ValueMap inputs = new ValueMap(
                "TaskInput", new ValueMap(
                "Assignee", "me, myself and I",
                "Content", new ValueMap(
                "Subject", "Decide on this topic",
                "Decision", "Yet to be decided")
        ),
                "HTTPConfig", new ValueMap("port", port)
        );

        CaseTeam team = createCaseTeam(pete, gimy, TestScript.createOwner(tom));
        StartCase startCase = createCaseCommand(pete, caseInstanceId, xml, inputs, team);
        testCase.addStep(startCase, cp -> {
            // Depending on how fast the first (process) task starts, the "HumanTask" is either Active or still Available
            String taskId = cp.assertPlanItem("HumanTask").getId();

            TaskOutputFilled tof = testCase.getEventListener().awaitTaskOutputFilled("AssertMockServiceIsRunning", e -> true);
            long responseCode = tof.getTaskOutputParameters().raw("responseCode");
            TestScript.debugMessage("Ping mock service gave response code " + responseCode);

            //
            // SaveTaskOutput - User should be able to save the task output for Unassigned task
            //
            testCase.addStep(new SaveTaskOutput(pete, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action -> {
                action.getEvents().assertEventType(HumanTaskOutputSaved.class, 1);
                action.getEvents().assertNotEmpty();
            });

            //
            // ClaimTask - User should be able to claim the task
            //
            testCase.addStep(new ClaimTask(pete, caseInstanceId, taskId), action -> {
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                taskAssertion.assertAssignee("pete");
            });

            //
            // ValidateTaskOutput - Task output validation fails on wrong user
            //
            testCase.addStep(new ValidateTaskOutput(gimy, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()));

            //
            // ValidateTaskOutput - Task output validation should result in a failure when we send "KILLSWITCH"
            //
            testCase.assertStepFails(new ValidateTaskOutput(pete, caseInstanceId, taskId, taskOutputFailingValidation.cloneValueNode()),
                    failure -> failure.assertException("Unexpected http response code 500"));

            //
            // ValidateTaskOutput - Task output validation fails on wrong output
            //
            testCase.addStep(new ValidateTaskOutput(pete, caseInstanceId, taskId, taskOutputInvalidDecision.cloneValueNode()), action -> {
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                Value<?> jsonResponse = taskAssertion.getValidationResponse().toJson();
                if (!(jsonResponse.isMap())) {
                    throw new AssertionError("Expecting a ValueMap response from Task Validation, but received something of type " + jsonResponse.getClass().getName());
                }
                if (!jsonResponse.asMap().equals(invalidDecisionResponse)) {
                    TestScript.debugMessage("Would expect an OK Status in\n" + jsonResponse);
                    throw new AssertionError("Would expect a OK here");
                }

                TestScript.debugMessage("TaskOutputValidation resulted in errors: " + jsonResponse);

//                taskAssertion.assertPlanItem("HumanTask").assertLastTransition(Transition.Start);
                // Task Output Validation should not lead to new events in the event log.
                action.getTestCommand().getEvents().assertSize(0);
            });

            //
            // ValidateTaskOutput - Task output validation should be ok with decision canceled
            //
            testCase.addStep(new ValidateTaskOutput(pete, caseInstanceId, taskId, taskOutputDecisionCanceled.cloneValueNode()), action -> {
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                Value<?> jsonResponse = taskAssertion.getValidationResponse().toJson();
                if (!(jsonResponse.isMap())) {
                    throw new AssertionError("Expecting a ValueMap response from Task Validation, but received something of type " + jsonResponse.getClass().getName());
                }
                if (!jsonResponse.equals(validDecisionResponse)) {
                    TestScript.debugMessage("Would expect a OK Status in\n" + jsonResponse);
                    throw new AssertionError("Would expect a OK here");
                }
                testCase.getEventListener().getNewEvents().assertSize(0);
            });

            //
            // ValidateTaskOutput - Task output validation should be ok with decision approved, and it should not lead to new events
            //
            testCase.addStep(new ValidateTaskOutput(pete, caseInstanceId, taskId, taskOutputDecisionApproved.cloneValueNode()), action -> action.getEvents().assertSize(0));

            //
            // SaveTaskOutput - User should be able to save the task with invalid output, and it should lead to new events
            //
            testCase.addStep(new SaveTaskOutput(pete, caseInstanceId, taskId, taskOutputInvalidDecision.cloneValueNode()), action -> {
//                casePlan.assertPlanItem("HumanTask").assertLastTransition(Transition.Start);
                action.getEvents().assertEventType(HumanTaskOutputSaved.class, 1);
                action.getEvents().assertNotEmpty();
            });

            //
            // CompleteTaskOutput - User should not be able to complete the task with invalid output
            //
            testCase.assertStepFails(new CompleteHumanTask(pete, caseInstanceId, taskId, taskOutputInvalidDecision.cloneValueNode()),
                    failure -> failure.assertException(InvalidCommandException.class, "Output for task HumanTask is invalid"));

            //
            // CompleteTask - Only the current task assignee should be able to complete the task
            //
            testCase.addStep(new CompleteHumanTask(pete, caseInstanceId, taskId, taskOutputDecisionApproved.cloneValueNode()), action -> {
                HumanTaskAssertion taskAssertion = new HumanTaskAssertion(action);
                testCase.getEventListener().awaitTaskOutputFilled(taskId, taskEvent -> {
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

    private void startWireMocks() {
        //Start wireMock service for mocking process service calls
        wireMockRule.stubFor(post(urlEqualTo("/validate"))
                .withHeader("Accept", equalTo("application/json"))
                .andMatching(request -> matchesValidDecision(request))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(validDecisionResponse.toString())));
        wireMockRule.stubFor(post(urlEqualTo("/validate"))
                .withHeader("Accept", equalTo("application/json"))
                .andMatching(request -> matchesInvalidDecision(request))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(invalidDecisionResponse.toString())));
        wireMockRule.stubFor(post(urlEqualTo("/validate"))
                .withHeader("Accept", equalTo("application/json"))
                .andMatching(request -> matchesInvalidRequest(request))
                .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR).withBody("Something went really wrong in here")));
        wireMockRule.stubFor(get(urlEqualTo("/ping")).willReturn(aResponse().withStatus(200)));
    }

    private MatchResult matchesValidDecision(Request request) {
        try {
            ValueMap json = JSONReader.parse(request.getBody());
            json = json.with("task-output");
            if (json.equals(taskOutputDecisionApproved) || json.equals(taskOutputDecisionCanceled)) {
                TestScript.debugMessage("\n\nmatching - valid decision\n\n");
                return MatchResult.of(true);
            }
            TestScript.debugMessage("\n\nNO MATCH on valid decision\n\n");
            return MatchResult.of(false);
        } catch (IOException | JSONParseFailure e) {
            TestScript.debugMessage("Invalid JSON");
            return MatchResult.of(false);
        }
    }

    private MatchResult matchesInvalidDecision(Request request) {
        try {
            ValueMap json = JSONReader.parse(request.getBody());
            json = json.with("task-output");
            if (json.equals(taskOutputDecisionApproved) || json.equals(taskOutputDecisionCanceled) || json.equals(taskOutputFailingValidation)) {
                TestScript.debugMessage("\n\nNO MATCH on invalid decision\n\n");
                return MatchResult.of(false);
            }
            TestScript.debugMessage("\n\nmatching - invalid decision\n\n");
            return MatchResult.of(true);
        } catch (IOException | JSONParseFailure e) {
            TestScript.debugMessage("Invalid JSON");
            return MatchResult.of(false);
        }
    }

    private MatchResult matchesInvalidRequest(Request request) {
        try {
            ValueMap json = JSONReader.parse(request.getBody());
            json = json.with("task-output");
            if (json.equals(taskOutputFailingValidation)) {
                TestScript.debugMessage("\n\nmatching - Failing on validation\n\n");
                return MatchResult.of(true);
            } else {
                TestScript.debugMessage("\n\nNO MATCH on 'Failing on validation'\n\n");
            }
            return MatchResult.of(false);
        } catch (IOException | JSONParseFailure e) {
            TestScript.debugMessage("Invalid JSON");
            return MatchResult.of(true);
        }
    }
}
