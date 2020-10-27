/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.casefile;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.item.CreateCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.item.DeleteCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.item.ReplaceCaseFileItem;
import org.cafienne.cmmn.akka.command.casefile.item.UpdateCaseFileItem;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileError;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.util.Guid;
import org.junit.Test;

public class BasicTypes {
    // This tests a set of basic case file types and properties
    // The test just starts the case and then validates the output, no specific actions are done (no transitions are made)

    private final String caseName = "CaseFileDefinitionTest";
    private final String inputParameterName = "inputCaseFile";
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/casefile/basictypes.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    private ValueMap getInputs() {
        ValueMap inputs = new ValueMap();
        inputs.put(inputParameterName, getContents());
        return inputs;
    }

    private ValueMap getContents(ValueMap... inputs) {
        if (inputs.length > 0) {
            return (ValueMap) inputs[0].get(inputParameterName);
        }
        ValueMap contents = new ValueMap();
        // Put some contents in the input parameter
        contents.putRaw("aString", "My favorite first string");
        contents.putRaw("aBoolean", true);
        contents.putRaw("aDate", "2001-10-26");
        contents.putRaw("aDateTime", "2001-10-26T21:32:52");
        contents.putRaw("aDuration", "PT2S");
        contents.putRaw("aGDay", "---03");
        contents.putRaw("aGMonthDay", "--05-01");
        contents.putRaw("aGMonth", "--05");
        contents.putRaw("aGYear", "-2003");
        contents.putRaw("aGYearMonth", "2001-10");
        contents.putRaw("anInteger", 10);
        contents.putRaw("aDouble", 2.0);
        contents.putRaw("aDecimal", 0.1);
        return contents;
    }

    @Test
    public void testBasicCaseFileDefinitionInput() {
        // TODO: the initial command results in an exception on setting the parameter (we do also a NegativeTest).
        // However, internally the engine has already processed the setting of the definition. So this need not be done again.
        // But we do in the next startCase statement. Subsequently the engine complains that the case definition has already been set.
        // We overcome this failure by starting with a new case id.
        // However, instead of that behavior we should make the engine allow for properly restarting the case and allowing us to use the same id.
        // TODO: put this in a different test. Here we're testing case file, not engine robustness

        String caseInstanceId = new Guid().toString();
        TestScript testCase = new TestScript(caseName);

        ValueMap inputs = getInputs();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs.cloneValueNode(), null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes").assertValue(getContents(inputs)));

        testCase.runTest();
    }

    @Test
    public void testInvalidInputParameterName() {
        TestScript testCase = new TestScript(caseName);
        String caseInstanceId = "CaseFileDefinitionTest";

        // First we do a test with wrong input parameter names
        String wrongParameterName = "wrong-inputCaseFile";

        ValueMap inputs = getInputs();
        Value<?> contents = inputs.getValue().remove(inputParameterName);
        inputs.put(wrongParameterName, contents);

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs.cloneValueNode(), null);
        testCase.assertStepFails(startCase, action -> action.assertException(Exception.class, "An input parameter with name " + wrongParameterName + " is not defined in the case"));

        testCase.runTest();
    }

    @Test
    public void testInvalidInputProperties() {
        TestScript testCase = new TestScript(caseName);

        // Now test a whole series of invalid values for the properties.
        // Most notably this ought to check the StringValue match validations for date/time.
        // But furthermore also number validations.
        testInvalidValueForProperty(testCase, "aBoolean", "string that is not a boolean");
        testInvalidValueForProperty(testCase, "aDate", "2001-10-32"); // Putting a wrong date
        testInvalidValueForProperty(testCase, "aDate", "2001-10-31T"); // Putting a wrong date
        testInvalidValueForProperty(testCase, "aDateTime", "2001-10-26T"); // Putting a wrong date, nothing behind the T
        testInvalidValueForProperty(testCase, "aDuration", "PT2MS");
        testInvalidValueForProperty(testCase, "aGDay", "---32"); // Putting invalid day
        testInvalidValueForProperty(testCase, "aGMonthDay", "--13-01"); // Putting invalid month
        testInvalidValueForProperty(testCase, "aGMonth", "---13"); // Putting a day instead of a month
        testInvalidValueForProperty(testCase, "aGYear", "---03"); // Putting a day instead of a year
        testInvalidValueForProperty(testCase, "aGYearMonth", "--05"); // Putting a month instead of year+month
        testInvalidValueForProperty(testCase, "anInteger", 2.0); // Putting a decimal instead of int

        testCase.runTest();
    }

    private void testInvalidValueForProperty(TestScript testCase, String propertyName, Object propertyValue) {
        ValueMap inputs = getInputs();
        ValueMap contents = getContents(inputs);
        // Make a clone of contents and enter the wrong property
        ValueMap wrongContents = contents.cloneValueNode();
        wrongContents.putRaw(propertyName, propertyValue);
        // And now clone the inputs and put the wrong parameter in it
        ValueMap wrongInputs = inputs.cloneValueNode();
        wrongInputs.put(inputParameterName, wrongContents);

        String caseInstanceId = "TestingWrongProperty" + propertyName + "_" + new Guid(); // Make a new CaseInstanceId to overcome framework
        // limitation.
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, wrongInputs.cloneValueNode(), null);
        testCase.assertStepFails(startCase, action -> action.assertException(CaseFileError.class, "Property '" + propertyName + "' has wrong type"));
    }

    @Test
    public void testComplexInputParameter() {
        TestScript testCase = new TestScript(caseName);

        ValueMap inputs = getInputs();
        ValueMap contents = getContents(inputs);

        // Now start a case with a child being set within the JSON input
        ValueMap childItem = new ValueMap();
        childItem.putRaw("aChildString", "child-string");
        childItem.putRaw("aSecondChildString", "child-string-2");

        contents.put("ChildItem", childItem);
        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs.cloneValueNode(), null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(childItem));

        testCase.runTest();
    }

    @Test
    public void testBasicOperations() {
        TestScript testCase = new TestScript(caseName);

        ValueMap inputs = getInputs();
        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs.cloneValueNode(), null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(Value.NULL));

        // Now start a case with a child being set within the JSON input
        ValueMap childItem = new ValueMap();
        childItem.putRaw("aChildString", "child-string");
        childItem.putRaw("aSecondChildString", "child-string-2");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "AllPropertyTypes/ChildItem");
        testCase.addStep(createChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(childItem));

        // Now update the child item with new content: one property has a new value, another property is not updated, and a third property is newly
        // inserted
        // Effectively this should merge old and new content.
        ValueMap updatedContent = new ValueMap();
        updatedContent.putRaw("aChildString", "aNewChildString");
        updatedContent.putRaw("aChildInteger", 9);

        // Create a clone an merge old and new content to create a new object that is expected to be the result of the UpdateCaseFileItem operation
        ValueMap childItemClone = childItem.cloneValueNode();
        final ValueMap expectedChildContent = (ValueMap) childItemClone.merge(updatedContent);

        UpdateCaseFileItem updateChild = new UpdateCaseFileItem(testUser, caseInstanceId, updatedContent.cloneValueNode(), "AllPropertyTypes/ChildItem");
        testCase.addStep(updateChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(expectedChildContent));

        // Now replace the child item.
        ValueMap replacedChildItem = new ValueMap();
        replacedChildItem.putRaw("aChildInteger", 3);
        ReplaceCaseFileItem replaceChild = new ReplaceCaseFileItem(testUser, caseInstanceId, replacedChildItem.cloneValueNode(), "AllPropertyTypes/ChildItem");
        testCase.addStep(replaceChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(replacedChildItem));

        // Now delete the child item.
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, "AllPropertyTypes/ChildItem");
        testCase.addStep(deleteChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(Value.NULL));

        testCase.runTest();
    }

    @Test
    public void testBasicOperationFailures() {
        TestScript testCase = new TestScript(caseName);

        ValueMap inputs = getInputs();
        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs.cloneValueNode(), null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(Value.NULL));

        // Create a child item; child item contains an invalid value, so it should reject the operation, and child should still be null
        ValueMap childItem = new ValueMap();
        childItem.putRaw("aChildString", "child-string");
        childItem.putRaw("aChildInteger", "I should be an integer");
        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "AllPropertyTypes/ChildItem");
        testCase.assertStepFails(createChild);

        // Update the child item; Should fail because child is still not created
        ValueMap updatedContent = new ValueMap();
        updatedContent.putRaw("aChildString", "aNewChildString");
        updatedContent.putRaw("aChildInteger", "I should be an integer");

        UpdateCaseFileItem updateChild = new UpdateCaseFileItem(testUser, caseInstanceId, updatedContent.cloneValueNode(), "AllPropertyTypes/ChildItem");
        testCase.assertStepFails(updateChild);

        // Replace the child item; Should fail because child is still not created
        ValueMap replacedChildItem = new ValueMap();
        replacedChildItem.putRaw("aChildInteger", "I should be an integer");
        ReplaceCaseFileItem replaceChild = new ReplaceCaseFileItem(testUser, caseInstanceId, replacedChildItem.cloneValueNode(), "AllPropertyTypes/ChildItem");
        testCase.assertStepFails(replaceChild);

        // Now delete the child item.
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, "AllPropertyTypes/ChildItem");
        testCase.assertStepFails(deleteChild);

        // Now create a proper child and then we should fail in updating and replacing it

        // Create a child item; child item contains an invalid value, so it should reject the operation, and child should still be null
        childItem.putRaw("aChildString", "child-string");
        childItem.putRaw("aChildInteger", 9);
        createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "AllPropertyTypes/ChildItem");
        testCase.addStep(createChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(childItem));

        // Again try to update the child item; Should fail because command has invalid content
        updateChild = new UpdateCaseFileItem(testUser, caseInstanceId, updatedContent.cloneValueNode(), "AllPropertyTypes/ChildItem");
        testCase.assertStepFails(updateChild);

        replaceChild = new ReplaceCaseFileItem(testUser, caseInstanceId, replacedChildItem.cloneValueNode(), "AllPropertyTypes/ChildItem");
        // And again try to replace the child item; Should fail because command has invalid content
        testCase.assertStepFails(replaceChild);

        deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, "AllPropertyTypes/ChildItem");
        // Now delete the child item again. That should work.
        testCase.addStep(deleteChild, casePlan -> casePlan.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(Value.NULL));

        testCase.runTest();
    }

    @Test
    public void testArrayOperations() {
        TestScript testCase = new TestScript(caseName);
        ValueMap inputs = getInputs();
        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs.cloneValueNode(), null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(Value.NULL));

        // Now start a case with a child being set within the JSON input
        ValueMap childItem = new ValueMap();
        childItem.putRaw("aChildString", "child-string");
        childItem.putRaw("aSecondChildString", "child-string-2");

        ValueList arrayOfChildItem = new ValueList();
        arrayOfChildItem.add(childItem.cloneValueNode());

        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "AllPropertyTypes/ArrayOfChildItem");
        testCase.addStep(createChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem").assertValue(arrayOfChildItem).assertCaseFileItem("[0]").assertValue(childItem));

        // Now update the child item with new content: one property has a new value, another property is not updated, and a third property is newly
        // inserted
        // Effectively this should merge old and new content.
        ValueMap updatedContent = new ValueMap();
        updatedContent.putRaw("aChildString", "aNewChildString");
        updatedContent.putRaw("aChildInteger", 9);

        // Create a clone an merge old and new content to create a new object that is expected to be the result of the UpdateCaseFileItem operation
        ValueMap childItemClone = childItem.cloneValueNode();
        final ValueMap expectedChildContent = (ValueMap) childItemClone.merge(updatedContent);

        UpdateCaseFileItem updateChild = new UpdateCaseFileItem(testUser, caseInstanceId, updatedContent.cloneValueNode(), "AllPropertyTypes/ArrayOfChildItem[0]");
        testCase.addStep(updateChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem[0]").assertValue(expectedChildContent));

        // Now replace the child item.
        ValueMap replacedChildItem = new ValueMap();
        replacedChildItem.putRaw("aChildInteger", 3);
        ReplaceCaseFileItem replaceChild = new ReplaceCaseFileItem(testUser, caseInstanceId, replacedChildItem.cloneValueNode(), "AllPropertyTypes/ArrayOfChildItem[0]");
        testCase.addStep(replaceChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem[0]").assertValue(replacedChildItem));

        // Now delete the child item.
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, "AllPropertyTypes/ArrayOfChildItem[0]");
        testCase.addStep(deleteChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem[0]").assertValue(Value.NULL));

        testCase.runTest();
    }

    @Test
    public void testMoreArrayOperations() {
        TestScript testCase = new TestScript(caseName);

        ValueMap inputs = getInputs();
        String caseInstanceId = new Guid().toString();
        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs.cloneValueNode(), null);
        testCase.addStep(startCase, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ChildItem").assertValue(Value.NULL));

        // Now start a case with a child being set within the JSON input
        ValueMap childItem = new ValueMap();
        childItem.putRaw("aChildString", "child-string");
        childItem.putRaw("aSecondChildString", "child-string-2");

        ValueList arrayOfChildItem = new ValueList();
        arrayOfChildItem.add(childItem.cloneValueNode());

        CreateCaseFileItem createChild = new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "AllPropertyTypes/ArrayOfChildItem");
        testCase.addStep(createChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem").assertValue(arrayOfChildItem).assertCaseFileItem("[0]").assertValue(childItem));

        ValueList secondArray = arrayOfChildItem.cloneValueNode();
        secondArray.add(childItem.cloneValueNode());

        // add another child
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, childItem.cloneValueNode(), "AllPropertyTypes/ArrayOfChildItem"), caseFile -> {
            caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem").assertSize(2).assertValue(secondArray);
            caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem[0]").assertValue(childItem);
            caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem[1]").assertValue(childItem);
            // TODO When try to a access non index value it will return IndexOutOfBoundsException.
            //caseFile.assertCaseFileItems("AllPropertyTypes/ArrayOfChildItem").assertValue(Value.NULL, 2);
        });


        // Now update the child item with new content: one property has a new value, another property is not updated, and a third property is newly
        // inserted
        // Effectively this should merge old and new content.
        ValueMap updatedContent = new ValueMap();
        updatedContent.putRaw("aChildString", "aNewChildString");
        updatedContent.putRaw("aChildInteger", 9);

        // Create a clone an merge old and new content to create a new object that is expected to be the result of the UpdateCaseFileItem operation
        ValueMap childItemClone = childItem.cloneValueNode();
        final ValueMap expectedChildContent = (ValueMap) childItemClone.merge(updatedContent);

        UpdateCaseFileItem updateChild = new UpdateCaseFileItem(testUser, caseInstanceId, updatedContent.cloneValueNode(), "AllPropertyTypes/ArrayOfChildItem[0]");
        testCase.addStep(updateChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem[0]").assertValue(expectedChildContent));

        // Now replace the child item.
        ValueMap replacedChildItem = new ValueMap();
        replacedChildItem.putRaw("aChildInteger", 3);
        ReplaceCaseFileItem replaceChild = new ReplaceCaseFileItem(testUser, caseInstanceId, replacedChildItem.cloneValueNode(), "AllPropertyTypes/ArrayOfChildItem[0]");
        testCase.addStep(replaceChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem[0]").assertValue(replacedChildItem));

        // Now delete the child item.
        DeleteCaseFileItem deleteChild = new DeleteCaseFileItem(testUser, caseInstanceId, "AllPropertyTypes/ArrayOfChildItem[0]");
        testCase.addStep(deleteChild, caseFile -> caseFile.assertCaseFileItem("AllPropertyTypes/ArrayOfChildItem[0]").assertValue(Value.NULL));

        testCase.runTest();
    }
}
