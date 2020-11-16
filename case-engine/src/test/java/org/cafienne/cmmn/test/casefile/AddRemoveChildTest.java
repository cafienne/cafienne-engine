package org.cafienne.cmmn.test.casefile;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.DeleteCaseFileItem;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.util.Guid;
import org.junit.Test;

public class AddRemoveChildTest {
    // This tests a set of basic case file types and properties
    // The test just starts the case and then validates the output, no specific actions are done (no transitions are made)

    private final String caseName = "addChildTest";
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/addChildTest.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");
    private final Path testPath = new Path("test");
    private final Path testChildPath = new Path("test/testChild");

    @Test
    public void testAddAndRemoveChild() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(testPath).assertValue(Value.NULL));

        ValueMap item = new ValueMap();
        item.putRaw("effe", "root-string");
        item.putRaw("effe2", "root-string-2");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, testPath, item.cloneValueNode());
        testCase.addStep(createChild, caseFile -> {
            caseFile.assertCaseFileItem(testPath).assertValue(item);
            caseFile.assertCaseFileItem(testPath).assertState(State.Available);
        });


        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.putRaw("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, testChildPath, childItem.cloneValueNode());
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
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(testPath).assertValue(Value.NULL));

        ValueMap item = new ValueMap();
        item.putRaw("effe", "root-string");
        item.putRaw("effe2", "root-string-2");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, testPath, item.cloneValueNode());
        testCase.addStep(createChild, caseFile -> caseFile.assertCaseFileItem(testPath).assertValue(item));


        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.putRaw("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, testChildPath, childItem.cloneValueNode());
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
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(testPath).assertValue(Value.NULL));

        // Add child item together with parent
        ValueMap childItem = new ValueMap();
        childItem.putRaw("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, testChildPath, childItem.cloneValueNode());
        // In case the parent does not have state == Active, CaseFileItemChildAdded should not be triggered
        testCase.addStep(createChild2, caseFile -> caseFile.assertCaseFileItem(testPath).assertState(State.Null));

        testCase.runTest();
    }

}
