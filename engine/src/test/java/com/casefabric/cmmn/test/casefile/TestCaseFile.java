package com.casefabric.cmmn.test.casefile;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.json.ValueMap;
import com.casefabric.util.Guid;
import org.junit.Test;

import static com.casefabric.cmmn.test.TestScript.*;

public class TestCaseFile {
    private final String caseName = "CaseFileTest";
    private final String inputParameterName = "aaa";
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/casefile/casefiletest.xml");

    @Test
    public void testPropertyAccessingFromSentry() {

        String caseInstanceId = new Guid().toString();
        TestScript testCase = new TestScript(caseName);

        ValueMap valueAaa = new ValueMap();
        valueAaa.plus("aaa1", "true");

        ValueMap rootValue = new ValueMap();
        rootValue.put(inputParameterName, valueAaa);

        ValueMap childOfAaa1 = new ValueMap();
        childOfAaa1.plus("child_of_aaa_1", "true");

        valueAaa.put("child_of_aaa", childOfAaa1);

        TestScript.debugMessage(rootValue.toString());

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, rootValue.cloneValueNode());

        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            // First and second task should be active, because their ifParts are filled. Third task not.
            casePlan.assertTask("FirstTask").assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertTask("SecondTask").assertLastTransition(Transition.Start, State.Active, State.Available);
            casePlan.assertTask("ThirdTask").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        testCase.runTest();
    }

    @Test
    public void testInsufficientInputParameterValuesForSentryEvaluation() {

        String caseInstanceId = new Guid().toString();
        TestScript testCase = new TestScript(caseName);

        ValueMap rootValue = new ValueMap();
        ValueMap value1 = new ValueMap();
        rootValue.put(inputParameterName, value1);
        ValueMap x = new ValueMap();
        x.plus("y", "true");
        value1.plus("x", x);


        // This input leads to a crashing ifPart evaluation in the engine.
        //  That is actually incorrect behavior of the engine


        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, rootValue.cloneValueNode());
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            casePlan.assertTask("FirstTask").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertTask("SecondTask").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertTask("ThirdTask").assertLastTransition(Transition.Start, State.Active, State.Available);

        });

        testCase.runTest();
    }
}
