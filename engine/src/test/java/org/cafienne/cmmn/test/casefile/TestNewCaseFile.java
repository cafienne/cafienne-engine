package org.cafienne.cmmn.test.casefile;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.ReplaceCaseFileItem;
import org.cafienne.cmmn.actorapi.command.casefile.UpdateCaseFileItem;
import org.cafienne.cmmn.actorapi.event.file.CaseFileItemTransitioned;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.Path;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.file.CaseFileAssertion;
import org.cafienne.json.Value;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.cafienne.util.Guid;
import org.junit.Test;

import static org.cafienne.cmmn.test.TestScript.*;

public class TestNewCaseFile {
    private final String caseName = "NewCaseFileTest";
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/casefile/casefile.xml");

    private final Path rootPath = new Path("RootCaseFileItem");

    @Test
    public void testCreateAndModifyFullCaseFile() {

        String caseInstanceId = new Guid().toString();
        TestScript testCase = new TestScript(caseName);

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions);

        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
        });

        ValueMap caseFileItem = new ValueMap("RootProperty1", "string", "RootProperty2", true, "ChildItem", createFamily(), "ChildArray", new ValueList(createChildItem(), createChildItem()));
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, caseInstanceId, caseFileItem, rootPath), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(8);
        });

        // Clone entire structure, change a single property and then update should lead to only 1 update event
        ValueMap cfi2 = caseFileItem.cloneValueNode();
        cfi2.plus("RootProperty2", false);
        testCase.addStep(new UpdateCaseFileItem(testUser, caseInstanceId, caseInstanceId, cfi2, rootPath), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(2);
        });

        // Update the case file item with only a new value for "RootProperty1" should only change that field.
        ValueMap shallowValue = new ValueMap("RootProperty1", "Second String");
        testCase.addStep(new UpdateCaseFileItem(testUser, caseInstanceId, caseInstanceId, shallowValue, rootPath), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(2);
        });

        // Replacing with the shallow copy should replace entire file
        testCase.addStep(new ReplaceCaseFileItem(testUser, caseInstanceId, caseInstanceId, shallowValue, rootPath), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(4);
        });

        // Set back entire file should lead to 7 events again
        testCase.addStep(new ReplaceCaseFileItem(testUser, caseInstanceId, caseInstanceId, cfi2, rootPath), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(8);
        });

        // Update the case file item with changing property deeper in the structure.
        ValueMap updateDeeperProperty = cfi2.cloneValueNode();

        TestScript.debugMessage(updateDeeperProperty);

        ValueMap childItem = updateDeeperProperty.with("ChildItem");
        childItem.plus("ChildName", "First name update");
        childItem.plus("ChildAge", 33);
        ValueList grandChildArray = childItem.withArray("GrandChildArray");
        ValueMap firstGrandChildFromArray = grandChildArray.get(0).asMap();
        firstGrandChildFromArray.plus("GrandChildName", "Second name update");

        testCase.addStep(new UpdateCaseFileItem(testUser, caseInstanceId, caseInstanceId, updateDeeperProperty, rootPath), result -> {
            result.getEvents().printEventList();
            Value<?> v = new CaseFileAssertion(result.getTestCommand()).assertCaseFileItem(rootPath).getValue();
            result.getEvents().assertSize(3);
            result.getEvents().filter(CaseFileItemTransitioned.class).getEvents().forEach(event -> TestScript.debugMessage(event +" with value " + event.getValue()));
            TestScript.debugMessage("\n\nCF: " + v);
            result.print();

        });

        testCase.runTest();
    }

    ValueMap createFamily() {
        ValueMap family = createChildItem();
        family.put("GrandChildItem", createGrandChildItem());
        family.put("GrandChildArray", new ValueList(createGrandChildItem(), createGrandChildItem()));
        return family;
    }

    ValueMap createChildItem() {
        return new ValueMap("ChildName", "name", "ChildAge", 20);
    }

    ValueMap createGrandChildItem() {
        return new ValueMap("GrandChildName", "name", "GrandChildBirthDate", "2001-10-26");
    }

}

