package org.cafienne.cmmn.test.casefile;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.DeleteCaseFileItem;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.util.Guid;
import org.junit.Test;

public class AddRemoveChildTest {
    // This tests a set of basic case file types and properties
    // The test just starts the case and then validates the output, no specific actions are done (no transitions are made)

    private final String caseName = "addChildTest";
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/addChildTest.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testAddAndRemoveChild() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("test").assertValue(Value.NULL));

        ValueMap item = new ValueMap();
        item.putRaw("effe", "root-string");
        item.putRaw("effe2", "root-string-2");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, item.cloneValueNode(), "test");
        testCase.addStep(createChild, caseFile -> {
            caseFile.assertCaseFileItem("test").assertValue(item);
            caseFile.assertCaseFileItem("test").assertState(State.Available);
        });


        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.putRaw("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "test/testChild");
        testCase.addStep(createChild2, caseFile -> {
            caseFile.assertCaseFileItem("test/testChild").assertValue(childItem);
            caseFile.assertCaseFileItem("test/testChild").assertState(State.Available);
        });

        // Now delete the child item.
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, "test/testChild");
        testCase.addStep(deleteChild, caseFile -> {
            caseFile.assertCaseFileItem("test/testChild").assertValue(Value.NULL);
            caseFile.assertCaseFileItem("test/testChild").assertState(State.Discarded);
        });

        testCase.runTest();
    }

    @Test
    public void testAddChildAndRemoveParent() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("test").assertValue(Value.NULL));

        ValueMap item = new ValueMap();
        item.putRaw("effe", "root-string");
        item.putRaw("effe2", "root-string-2");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, item.cloneValueNode(), "test");
        testCase.addStep(createChild, caseFile -> caseFile.assertCaseFileItem("test").assertValue(item));


        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.putRaw("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "test/testChild");
        testCase.addStep(createChild2, caseFile -> caseFile.assertCaseFileItem("test/testChild").assertValue(childItem));

        // Now delete the parent item.
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, "test");
        testCase.addStep(deleteChild);

        testCase.runTest();
    }

    @Test
    public void testAddChildWithParent() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("test").assertValue(Value.NULL));

        // Add child item together with parent
        ValueMap childItem = new ValueMap();
        childItem.putRaw("world", "child-string");
        CreateCaseFileItem createChild2 = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "test/testChild");
        // In case the parent does not have state == Active, CaseFileItemChildAdded should not be triggered
        testCase.addStep(createChild2, caseFile -> caseFile.assertCaseFileItem("test").assertState(State.Null));

        testCase.runTest();
    }

}
