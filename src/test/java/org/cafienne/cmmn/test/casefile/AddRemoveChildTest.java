package org.cafienne.cmmn.test.casefile;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.DeleteCaseFileItem;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.util.Guid;
import org.junit.Test;

import static org.cafienne.cmmn.test.TestScript.*;

public class AddRemoveChildTest {
    // This tests a set of basic case file types and properties
    // The test just starts the case and then validates the output, no specific actions are done (no transitions are made)

    private final String caseName = "addChildTest";
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/addChildTest.xml");
    private final Path testPath = new Path("test");
    private final Path testChildPath = new Path("test/testChild");

    @Test
    public void testAddAndRemoveChild() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(testPath).assertValue(Value.NULL));

        ValueMap item = new ValueMap();
        item.plus("effe", "root-string");
        item.plus("effe2", "root-string-2");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, item.cloneValueNode(), testPath);
        testCase.addStep(createChild, caseFile -> {
            caseFile.assertCaseFileItem(testPath).assertValue(item);
            caseFile.assertCaseFileItem(testPath).assertState(State.Available);
        });


        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.plus("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), testChildPath);
        testCase.addStep(createChild2, caseFile -> {
            caseFile.assertCaseFileItem(testChildPath).assertValue(childItem);
            caseFile.assertCaseFileItem(testChildPath).assertState(State.Available);
        });

        // Now delete the child item.
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, testChildPath);
        testCase.addStep(deleteChild, caseFile -> {
            caseFile.assertCaseFileItem(testChildPath).assertValue(Value.NULL);
            caseFile.assertCaseFileItem(testChildPath).assertState(State.Discarded);
        });

        testCase.runTest();
    }

    @Test
    public void testAddChildAndRemoveParent() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(testPath).assertValue(Value.NULL));

        ValueMap item = new ValueMap();
        item.plus("effe", "root-string");
        item.plus("effe2", "root-string-2");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, item.cloneValueNode(), testPath);
        testCase.addStep(createChild, caseFile -> caseFile.assertCaseFileItem(testPath).assertValue(item));


        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.plus("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), testChildPath);
        testCase.addStep(createChild2, caseFile -> caseFile.assertCaseFileItem(testChildPath).assertValue(childItem));

        // Now delete the parent item.
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, testPath);
        testCase.addStep(deleteChild);

        testCase.runTest();
    }

    @Test
    public void testAddChildWithParent() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(testPath).assertValue(Value.NULL));

        // Add child item together with parent
        ValueMap childItem = new ValueMap();
        childItem.plus("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), testChildPath);
        // In case the parent does not have state == Active, CaseFileItemChildAdded should not be triggered
        testCase.assertStepFails(createChild2);

        testCase.runTest();
    }

}
