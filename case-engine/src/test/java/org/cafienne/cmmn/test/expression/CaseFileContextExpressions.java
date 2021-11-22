package org.cafienne.cmmn.test.expression;


import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.TestUser;
import org.cafienne.json.Value;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.cafienne.util.Guid;
import org.junit.Test;

public class CaseFileContextExpressions {
    private final String caseName = "CaseFileContextExpressions";
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/expression/casefilecontextexpressions.xml");
    private final TestUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testContextSettingsFromTasks() {

        // Basically this tests input parameter mapping
        String caseInstanceId = new Guid().toString();
        TestScript testCase = new TestScript(caseName);

        ValueMap child1 = new ValueMap("arrayProp1", "child1");
        ValueMap child2 = new ValueMap("arrayProp1", "child2");

        ValueMap caseInput = new ValueMap("Container", new ValueMap("Child", new ValueList(child1, child2)));

        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions, caseInput.cloneValueNode());
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            testCase.getEventListener().awaitTaskInputFilled("TaskWithExpression", taskEvent -> {
                ValueMap taskInput = taskEvent.getMappedInputParameters();
                TestScript.debugMessage("Mapped input: " + taskInput);

                ValueMap inputObject = taskInput.with("Input");
                Value<?> expectingChild2 = inputObject.get("arrayProp1");
                if (! expectingChild2.getValue().equals("child2")) {
                    throw new AssertionError("Expecting child2 inside arrayProp1, but found " + expectingChild2);
                }
                Value<?> arrayLength = taskInput.get("Input2");
                if (!arrayLength.getValue().equals(2L)) {
                    throw new AssertionError("Expecting 2L to be the value of Input2, but found " + arrayLength);
                }
                return true;
            });
        });

        testCase.runTest();
    }
}
