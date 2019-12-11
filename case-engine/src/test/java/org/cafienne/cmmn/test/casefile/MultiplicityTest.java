package org.cafienne.cmmn.test.casefile;

import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.DeleteCaseFileItem;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.util.Guid;
import org.junit.Test;


public class MultiplicityTest {
    // This tests a set of basic case file types and properties
    // The test just starts the case and then validates the output, no specific actions are done (no transitions are made)

    private final String caseName = "multiTest";
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/multiTest.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testAddAndRemoveMultpilictyChildren() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addTestStep(startCase, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent").assertValue(Value.NULL);
        });


        // Add parent item
        ValueMap parentItem = new ValueMap();
        CreateCaseFileItem createParent = new CreateCaseFileItem(testUser, caseInstanceId, parentItem.cloneValueNode(), "parent");
        testCase.addTestStep(createParent, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent").assertState(State.Available);
        });

        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.putRaw("myprop", "some parent value");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "parent/child");
        testCase.addTestStep(createChild, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent/child").assertValue(childItem);
            caseFile.assertCaseFileItem("parent/child").assertState(State.Available);
        });


        ValueMap mchildItem2 = new ValueMap();
        mchildItem2.putRaw("mprop", "some value");
        CreateCaseFileItem mcreateChild2 = new CreateCaseFileItem(testUser, caseInstanceId, mchildItem2.cloneValueNode(), "parent/mchild");
        testCase.addTestStep(mcreateChild2, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent/mchild[0]").assertValue(mchildItem2);
            caseFile.assertCaseFileItem("parent/mchild[0]").assertState(State.Available);
        });

        ValueMap mchildItem = new ValueMap();
        mchildItem.putRaw("mprop", "some other value");
        CreateCaseFileItem mcreateChild = new CreateCaseFileItem(testUser, caseInstanceId, mchildItem.cloneValueNode(), "parent/mchild");
        testCase.addTestStep(mcreateChild, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent/mchild[1]").assertValue(mchildItem);
            caseFile.assertCaseFileItem("parent/mchild[1]").assertState(State.Available);
        });


        // Now delete the item
        DeleteCaseFileItem deleteChild0 = new DeleteCaseFileItem(testUser, caseInstanceId, "parent/mchild[0]");
        testCase.addTestStep(deleteChild0, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent/mchild[0]").assertState(State.Discarded);
        });
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, "parent/mchild[1]");
        testCase.addTestStep(deleteChild, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent/mchild[1]").assertState(State.Discarded);
        });


        testCase.runTest();
    }

    @Test
    public void testAddAndRemoveParentNoMultiplicity() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addTestStep(startCase, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent").assertValue(Value.NULL);
        });


        // Add parent item
        ValueMap parentItem = new ValueMap();
        CreateCaseFileItem createParent = new CreateCaseFileItem(testUser, caseInstanceId, parentItem.cloneValueNode(), "parent");
        testCase.addTestStep(createParent, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent").assertState(State.Available);
        });

        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.putRaw("myprop", "some parent value");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "parent/child");
        testCase.addTestStep(createChild, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent/child").assertValue(childItem);
            caseFile.assertCaseFileItem("parent/child").assertState(State.Available);
        });

        // Now delete the parent item
        DeleteCaseFileItem deleteParent = new DeleteCaseFileItem(testUser, caseInstanceId, "parent");
        testCase.addTestStep(deleteParent, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent").assertState(State.Discarded);
            caseFile.assertCaseFileItem("parent/child").assertState(State.Discarded);
        });

        testCase.runTest();
    }


    @Test
    public void testAddAndRemoveParent() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addTestStep(startCase, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent").assertValue(Value.NULL);
        });


        // Add parent item
        ValueMap parentItem = new ValueMap();
        CreateCaseFileItem createParent = new CreateCaseFileItem(testUser, caseInstanceId, parentItem.cloneValueNode(), "parent");
        testCase.addTestStep(createParent, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent").assertState(State.Available);
        });


        ValueMap mchildItem2 = new ValueMap();
        mchildItem2.putRaw("mprop", "some value");
        CreateCaseFileItem mcreateChild2 = new CreateCaseFileItem(testUser, caseInstanceId, mchildItem2.cloneValueNode(), "parent/mchild");
        testCase.addTestStep(mcreateChild2, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent/mchild[0]").assertValue(mchildItem2);
            caseFile.assertCaseFileItem("parent/mchild[0]").assertState(State.Available);
        });

        ValueMap mchildItem = new ValueMap();
        mchildItem.putRaw("mprop", "some other value");
        CreateCaseFileItem mcreateChild = new CreateCaseFileItem(testUser, caseInstanceId, mchildItem.cloneValueNode(), "parent/mchild");
        testCase.addTestStep(mcreateChild, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent/mchild[1]").assertValue(mchildItem);
            caseFile.assertCaseFileItem("parent/mchild[1]").assertState(State.Available);
        });


        // Now delete the parent item
        DeleteCaseFileItem deleteParent = new DeleteCaseFileItem(testUser, caseInstanceId, "parent");
        testCase.addTestStep(deleteParent, action -> {
            CaseAssertion caseFile = new CaseAssertion(action);
            caseFile.assertCaseFileItem("parent").assertState(State.Discarded);
        });


        testCase.runTest();
    }

}
