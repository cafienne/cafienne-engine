package com.casefabric.cmmn.test.basic;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.humantask.actorapi.command.CompleteHumanTask;
import com.casefabric.humantask.actorapi.event.HumanTaskAssigned;
import com.casefabric.humantask.actorapi.event.HumanTaskDueDateFilled;
import com.casefabric.json.ValueMap;
import org.junit.Test;

import static com.casefabric.cmmn.test.TestScript.*;

public class TestHelloWorld {
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/helloworld.xml");

    @Test
    public void testHelloWorld() {
        String caseInstanceId = "HelloWorldTest";
        TestScript testCase = new TestScript("hello-world");
        ValueMap greeting = new ValueMap("Greeting", new ValueMap("Message", "hello", "To", testUser.id(), "From", testUser.id()));

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, greeting);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            String taskId = casePlan.assertHumanTask("Receive Greeting and Send response").getId();
            casePlan.getEvents().filter(HumanTaskDueDateFilled.class).assertSize(1);
            casePlan.getEvents().filter(HumanTaskAssigned.class).assertSize(1);

            CompleteHumanTask completeTask1 = new CompleteHumanTask(testUser, caseInstanceId, taskId, new ValueMap());
            testCase.insertStep(completeTask1, casePlan2 -> {
                casePlan2.print();
                casePlan2.assertLastTransition(Transition.Create, State.Active, State.Null);
                casePlan2.assertPlanItem("Receive Greeting and Send response").assertState(State.Completed);
                casePlan2.assertPlanItem("Read response").assertState(State.Active);

            });
        });

        testCase.runTest();
    }

    @Test
    public void testHelloWorldWithoutAssignee() {
        String caseInstanceId = "HelloWorldTest";
        TestScript testCase = new TestScript("hello-world");
        ValueMap greeting = new ValueMap("Greeting", new ValueMap("Message", "hello", "To", "", "From", testUser.id()));

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, greeting);
        testCase.addStep(startCase, action -> {
            TestScript.debugMessage("Events: " + action.getTestCommand());
            action.getEvents().filter(HumanTaskDueDateFilled.class).assertSize(1);
            action.getEvents().filter(HumanTaskAssigned.class).assertSize(0);
        });

        testCase.runTest();
    }
}
