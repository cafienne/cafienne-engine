/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.casefile;

import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.akka.actor.identity.TenantUser;
import org.junit.Test;

public class RepetitiveFileItems {

    // Simple test for repetitive casefile structure
    private final String caseName = "repetitiveFileItems";
    private final TestScript testCase = new TestScript(caseName);
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/casefile/repeatcasefilecreation.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testRepetitiveFileItems() {
        // startCase
        String caseInstanceId = "CaseFileDefinitionTest";

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();

            // There must be 1 review task in state available and it must repeat.
            casePlan.assertPlanItems("Review").assertSize(1).assertStates(State.Available).assertRepeats();
        });

        // This test sets individual CaseFileItems
        ValueMap topCaseObject = new ValueMap();
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, topCaseObject.cloneValueNode(), "TopCase"), casePlan -> {
            casePlan.print();

            // There still must be 1 repeating review task in state available.
            casePlan.assertPlanItems("Review").assertSize(1).assertStates(State.Available).assertRepeats();

            casePlan.getEvents().assertCaseFileEvent("TopCase", e -> e.getValue().equals(topCaseObject));
            // There should be no items yet
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items");
        });

        // Create the CaseFileItem 'items' under the 'TopCase'; this is just an empty array
        ValueList itemArray = new ValueList();
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemArray.cloneValueNode(), "TopCase/items"), casePlan -> {
            casePlan.print();

            // Sending empty array contents should not result in new events;
            //   yes, i know, yes, this is little weird, but it is current engine behavior :(
            casePlan.getEvents().assertSize(0);
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items");
        });

        // And add a first item to 'items'
        ValueMap itemObject = new ValueMap();
        itemObject.putRaw("item", "Help, doe geen review");
        itemObject.putRaw("role", "expert");
        itemArray.add(itemObject); // Also add it to the test object

        // We're adding a clone of the item object into the case, so that we properly compare arrays. There must be 1 Review task.
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), "TopCase/items"), casePlan -> {
            casePlan.print();
            // There should be only 1 item in the case file, and it's value should be equal to the itemObject passed into it
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[0]", e -> e.getValue().equals(itemObject));
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[1]");

            // The entry criterion for the first review task must have been satisfied, so the Review task should be
            // in state active.
            casePlan.assertPlanItems("Review").assertSize(1).assertStates(State.Active).assertRepeats();

            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem("TopCase").assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent("TopCase");
        });

        // And add another item, causing changes in the plan (if it all works out). There now must be 2 Review tasks
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), "TopCase/items"), casePlan -> {
            casePlan.print();
//
//            i = 0;
//            System.out.println("New events: ");
//            testCase.getEventListener().getNewEvents().forEach(e -> {
//                System.out.println("Found "+(i++)+": "+e);
//            });
//            System.out.println("Done.");

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[1]", e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[0]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[2]");
            // Now there must be one more review task
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active);

            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem("TopCase").assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent("TopCase");
        });

        // And add another item, causing changes in the plan (if it all works out). There should not be any new Review tasks.
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), "TopCase/items"), casePlan -> {

            // Now the entry criterion of the last task has been met, and no more repetition, so no new tasks
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active).assertNoMoreRepetition();

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[2]", e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[0]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[1]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[3]");
            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem("TopCase").assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent("TopCase");
        });

        // And add another item, should not lead to new changes
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), "TopCase/items"), casePlan -> {

            // Now there must be still 3 active review tasks
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active).assertNoMoreRepetition();

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[3]", e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[0]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[1]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[2]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[4]");
            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem("TopCase").assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent("TopCase");
        });

        // And add another item, causing changes in the plan (if it all works out)
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), "TopCase/items"), casePlan -> {
            casePlan.print();

            // Now there must be still 3 active review tasks
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active);

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[4]", e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[0]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[1]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[2]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[3]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[5]");
            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem("TopCase").assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent("TopCase");
        });

        testCase.runTest();
    }

    @Test
    public void testRepetitiveFileItems23() {
        // startCase
        String caseInstanceId = "CaseFileDefinitionTest23";

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, null, null);
        testCase.addStep(startCase, casePlan -> {
            // There must be 1 review task in state available, and it must repeat because there are less than 2 item objects in the case file
            casePlan.assertPlanItems("Review").assertSize(1).assertStates(State.Available).assertRepeats();
        });

        // This test sets multiple CaseFileItems at once
        ValueMap topCaseObject = new ValueMap();
        ValueList itemArray = new ValueList();
        topCaseObject.put("items", itemArray);
        ValueMap itemObject = new ValueMap();
        itemObject.putRaw("item", "Help, doe geen review");
        itemObject.putRaw("role", "expert");
        itemArray.add(itemObject.cloneValueNode()); // [0]
        itemArray.add(itemObject.cloneValueNode()); // [1]

        // Creating 2 items in the array should trigger 2 new Review tasks
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, topCaseObject.cloneValueNode(), "TopCase"), casePlan -> {
            casePlan.print();
            // There must be 2 review tasks
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active);

            casePlan.assertCaseFileItem("TopCase/items[1]").assertValue(itemObject);

            // First command should result in 2 array items under TopCase
            casePlan.assertCaseFileItem("TopCase").assertValue(topCaseObject);
            casePlan.assertCaseFileItem("TopCase/items[0]").assertValue(itemObject);
            casePlan.assertCaseFileItem("TopCase/items[1]").assertValue(itemObject);
            casePlan.getEvents().assertCaseFileEvent("TopCase", e -> e.getValue().equals(topCaseObject));
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[0]", e -> e.getValue().equals(itemObject));
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[1]", e -> e.getValue().equals(itemObject));
            // But nothing yet on the next item
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[2]");
        });

        // Add another item. There should not be a new Review task, and the repetition of the second Review task must be false.
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), "TopCase/items"), casePlan -> {
            casePlan.print();

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[2]", e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent("TopCase");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[0]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[1]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[3]");

            // There must be 2 review tasks, one active and one available, both repeating.
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active).assertNoMoreRepetition();
        });

        // And yet another item
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), "TopCase/items"), casePlan -> {
            casePlan.print();
            // There must be 2 review tasks, one active and one available.
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active).assertNoMoreRepetition();

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent("TopCase/items[3]", e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent("TopCase");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[0]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[1]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[2]");
            casePlan.getEvents().assertNoCaseFileEvent("TopCase/items[4]");

            // There should be total of 0 .. 3 events
            casePlan.assertCaseFileItem("TopCase/items[0]").assertValue(itemObject);
            casePlan.assertCaseFileItem("TopCase/items[1]").assertValue(itemObject);
            casePlan.assertCaseFileItem("TopCase/items[2]").assertValue(itemObject);
            casePlan.assertCaseFileItem("TopCase/items[3]").assertValue(itemObject);

        });

        testCase.runTest();
    }
}
