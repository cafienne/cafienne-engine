package org.cafienne.cmmn.test.casefile;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.item.CreateCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.item.ReplaceCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.item.UpdateCaseFileItem;
import org.cafienne.cmmn.akka.event.file.CaseFileEvent;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.file.CaseFileAssertion;
import org.cafienne.util.Guid;
import org.junit.Test;

public class NewCaseFileTest {
    private final String caseName = "NewCaseFileTest";
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/casefile/casefile.xml");
    private final TenantUser user = TestScript.getTestUser("Anonymous");

    @Test
    public void testCreateGrandChildWithoutFirstCreatingParents() {

        String caseInstanceId = new Guid().toString();
        TestScript testCase = new TestScript(caseName);

        StartCase startCase = new StartCase(user, caseInstanceId, definitions, new ValueMap(), null);

        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
        });

        ValueList grandChildren = new ValueList(createGrandChildItem(), createGrandChildItem());
        testCase.addStep(new CreateCaseFileItem(user, caseInstanceId, grandChildren, "RootCaseFileItem/ChildItem/GrandChildArray"), result -> {
            result.print();
            result.getEvents().printEventList();
            result.getEvents().assertSize(3);
        });

        testCase.runTest();
    }

    @Test
    public void testCreateAndModifyFullCaseFile() {

        String caseInstanceId = new Guid().toString();
        TestScript testCase = new TestScript(caseName);

        StartCase startCase = new StartCase(user, caseInstanceId, definitions, new ValueMap(), null);

        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
        });

        ValueMap caseFileItem = new ValueMap("RootProperty1", "string", "RootProperty2", true, "ChildItem", createFamily(), "ChildArray", new ValueList(createChildItem(), createChildItem()));
        testCase.addStep(new CreateCaseFileItem(user, caseInstanceId, caseFileItem, "RootCaseFileItem"), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(8);
        });

        // Clone entire structure, change a single property and then update should lead to only 1 update event
        ValueMap cfi2 = caseFileItem.cloneValueNode();
        cfi2.putRaw("RootProperty2", false);
        testCase.addStep(new UpdateCaseFileItem(user, caseInstanceId, cfi2, "RootCaseFileItem"), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(2);
        });

        // Update the case file item with only a new value for "RootProperty1" should only change that field.
        ValueMap shallowValue = new ValueMap("RootProperty1", "Second String");
        testCase.addStep(new UpdateCaseFileItem(user, caseInstanceId, shallowValue, "RootCaseFileItem"), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(2);
        });

        // Replacing with the shallow copy should replace entire file
        testCase.addStep(new ReplaceCaseFileItem(user, caseInstanceId, shallowValue, "RootCaseFileItem"), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(2);
        });

        // Set back entire file should lead to 7 events again
        testCase.addStep(new ReplaceCaseFileItem(user, caseInstanceId, cfi2, "RootCaseFileItem"), result -> {
            result.getEvents().printEventList();
            result.getEvents().assertSize(8);
        });

        // Update the case file item with changing property deeper in the structure.
        ValueMap updateDeeperProperty = cfi2.cloneValueNode();

        TestScript.debugMessage(updateDeeperProperty);

        ValueMap childItem = updateDeeperProperty.with("ChildItem");
        childItem.putRaw("ChildName", "First name update");
        childItem.putRaw("ChildAge", 33);
        ValueList grandChildArray = childItem.withArray("GrandChildArray");
        ValueMap firstGrandChildFromArray = (ValueMap) grandChildArray.get(0);
        firstGrandChildFromArray.putRaw("GrandChildName", "Second name update");

        testCase.addStep(new UpdateCaseFileItem(user, caseInstanceId, updateDeeperProperty, "RootCaseFileItem"), result -> {
            result.getEvents().printEventList();
            Value v = new CaseFileAssertion(result.getTestCommand()).assertCaseFileItem("RootCaseFileItem").getValue();
            result.getEvents().assertSize(3);
            result.getEvents().filter(CaseFileEvent.class).getEvents().forEach(event -> TestScript.debugMessage(event +" with value " + event.getValue()));
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

