package com.casefabric.cmmn.test.casefile;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import com.casefabric.cmmn.actorapi.command.casefile.DeleteCaseFileItem;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.Path;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;
import com.casefabric.util.Guid;
import org.junit.Test;

import static com.casefabric.cmmn.test.TestScript.*;


public class TestMultiplicity {
    // This tests a set of basic case file types and properties
    // The test just starts the case and then validates the output, no specific actions are done (no transitions are made)

    private final String caseName = "multiTest";
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/multiTest.xml");
    private final Path parentPath = new Path("parent");
    private final Path childPath = new Path("parent/child");
    private final Path mChildPath = new Path("parent/mchild");
    private final Path child0 = new Path("parent/mchild[0]");
    private final Path child1 = new Path("parent/mchild[1]");

    @Test
    public void testAddAndRemoveMultpilictyChildren() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(parentPath).assertValue(Value.NULL));


        // Add parent item
        ValueMap parentItem = new ValueMap();
        CreateCaseFileItem createParent = new CreateCaseFileItem(testUser, caseInstanceId, parentItem.cloneValueNode(), parentPath);
        testCase.addStep(createParent, caseFile -> caseFile.assertCaseFileItem(parentPath).assertState(State.Available));

        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.plus("myprop", "some parent value");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), childPath);
        testCase.addStep(createChild, caseFile -> {
            caseFile.assertCaseFileItem(childPath).assertValue(childItem);
            caseFile.assertCaseFileItem(childPath).assertState(State.Available);
        });


        ValueMap mchildItem2 = new ValueMap();
        mchildItem2.plus("mprop", "some value");
        CreateCaseFileItem mcreateChild2 = new CreateCaseFileItem(testUser, caseInstanceId, mchildItem2.cloneValueNode(), mChildPath);
        testCase.addStep(mcreateChild2, caseFile -> {
            caseFile.assertCaseFileItem(child0).assertValue(mchildItem2);
            caseFile.assertCaseFileItem(child0).assertState(State.Available);
        });

        ValueMap mchildItem = new ValueMap();
        mchildItem.plus("mprop", "some other value");
        CreateCaseFileItem mcreateChild = new CreateCaseFileItem(testUser, caseInstanceId, mchildItem.cloneValueNode(), mChildPath);
        testCase.addStep(mcreateChild, caseFile -> {
            caseFile.assertCaseFileItem(child1).assertValue(mchildItem);
            caseFile.assertCaseFileItem(child1).assertState(State.Available);
        });


        // Now delete the item
        DeleteCaseFileItem deleteChild0 = new DeleteCaseFileItem(testUser, caseInstanceId, child0);
        testCase.addStep(deleteChild0, caseFile -> caseFile.assertCaseFileItem(child0).assertState(State.Discarded));
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, child1);
        testCase.addStep(deleteChild, caseFile -> caseFile.assertCaseFileItem(child1).assertState(State.Discarded));


        testCase.runTest();
    }

    @Test
    public void testAddAndRemoveParentNoMultiplicity() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(parentPath).assertValue(Value.NULL));


        // Add parent item
        ValueMap parentItem = new ValueMap();
        CreateCaseFileItem createParent = new CreateCaseFileItem(testUser, caseInstanceId, parentItem.cloneValueNode(), parentPath);
        testCase.addStep(createParent, caseFile -> caseFile.assertCaseFileItem(parentPath).assertState(State.Available));

        // Add child item
        ValueMap childItem = new ValueMap();
        childItem.plus("myprop", "some parent value");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), childPath);
        testCase.addStep(createChild, caseFile -> {
            caseFile.assertCaseFileItem(childPath).assertValue(childItem);
            caseFile.assertCaseFileItem(childPath).assertState(State.Available);
        });

        // Now delete the parent item
        DeleteCaseFileItem deleteParent = new DeleteCaseFileItem(testUser, caseInstanceId, parentPath);
        testCase.addStep(deleteParent, caseFile -> {
            caseFile.assertCaseFileItem(parentPath).assertState(State.Discarded);
            caseFile.assertCaseFileItem(childPath).assertState(State.Discarded);
        });

        testCase.runTest();
    }


    @Test
    public void testAddAndRemoveParent() {
        TestScript testCase = new TestScript(caseName);

        String caseInstanceId = new Guid().toString();
        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem(parentPath).assertValue(Value.NULL));


        // Add parent item
        ValueMap parentItem = new ValueMap();
        CreateCaseFileItem createParent = new CreateCaseFileItem(testUser, caseInstanceId, parentItem.cloneValueNode(), parentPath);
        testCase.addStep(createParent, caseFile -> caseFile.assertCaseFileItem(parentPath).assertState(State.Available));


        ValueMap mchildItem2 = new ValueMap();
        mchildItem2.plus("mprop", "some value");
        CreateCaseFileItem mcreateChild2 = new CreateCaseFileItem(testUser, caseInstanceId, mchildItem2.cloneValueNode(), mChildPath);
        testCase.addStep(mcreateChild2, caseFile -> {
            caseFile.assertCaseFileItem(child0).assertValue(mchildItem2);
            caseFile.assertCaseFileItem(child0).assertState(State.Available);
        });

        ValueMap mchildItem = new ValueMap();
        mchildItem.plus("mprop", "some other value");
        CreateCaseFileItem mcreateChild = new CreateCaseFileItem(testUser, caseInstanceId, mchildItem.cloneValueNode(), mChildPath);
        testCase.addStep(mcreateChild, caseFile -> {
            caseFile.assertCaseFileItem(child1).assertValue(mchildItem);
            caseFile.assertCaseFileItem(child1).assertState(State.Available);
        });


        // Now delete the parent item
        DeleteCaseFileItem deleteParent = new DeleteCaseFileItem(testUser, caseInstanceId, parentPath);
        testCase.addStep(deleteParent, caseFile -> caseFile.assertCaseFileItem(parentPath).assertState(State.Discarded));


        testCase.runTest();
    }

}
