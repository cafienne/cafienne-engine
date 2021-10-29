/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.casefile;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.command.casefile.CreateCaseFileItem;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.junit.Test;

public class RepetitiveFileItems {

    // Simple test for repetitive casefile structure
    private final String caseName = "repetitiveFileItems";
    private final TestScript testCase = new TestScript(caseName);
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/casefile/repeatcasefilecreation.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");
    private final Path topPath = new Path("TopCase");
    private final Path itemsPath = new Path("TopCase/items");
    private final Path item0 = new Path("TopCase/items[0]");
    private final Path item1 = new Path("TopCase/items[1]");
    private final Path item2 = new Path("TopCase/items[2]");
    private final Path item3 = new Path("TopCase/items[3]");
    private final Path item4 = new Path("TopCase/items[4]");
    private final Path item5 = new Path("TopCase/items[5]");

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
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, topCaseObject.cloneValueNode(), topPath), casePlan -> {
            casePlan.print();

            // There still must be 1 repeating review task in state available.
            casePlan.assertPlanItems("Review").assertSize(1).assertStates(State.Available).assertRepeats();

            casePlan.getEvents().assertCaseFileEvent(topPath, e -> e.getValue().equals(topCaseObject));
            // There should be no items yet
            casePlan.getEvents().assertNoCaseFileEvent(itemsPath);
        });

        // Create the CaseFileItem 'items' under the 'TopCase'; this is just an empty array
        ValueList itemArray = new ValueList();
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemArray.cloneValueNode(), itemsPath), casePlan -> {
            casePlan.print();

            // Sending empty array contents should not result in new events;
            //   yes, i know, yes, this is little weird, but it is current engine behavior :(
            casePlan.getEvents().assertSize(0);
            casePlan.getEvents().assertNoCaseFileEvent(itemsPath);
        });

        // And add a first item to 'items'
        ValueMap itemObject = new ValueMap();
        itemObject.plus("item", "Help, doe geen review");
        itemObject.plus("role", "expert");
        itemArray.add(itemObject); // Also add it to the test object

        // We're adding a clone of the item object into the case, so that we properly compare arrays. There must be 1 Review task.
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), itemsPath), casePlan -> {
            casePlan.print();
            // There should be only 1 item in the case file, and it's value should be equal to the itemObject passed into it
            casePlan.getEvents().assertCaseFileEvent(item0, e -> e.getValue().equals(itemObject));
            casePlan.getEvents().assertNoCaseFileEvent(item1);

            // The entry criterion for the first review task must have been satisfied, so the Review task should be
            // in state active.
            casePlan.assertPlanItems("Review").assertSize(1).assertStates(State.Active).assertRepeats();

            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem(topPath).assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent(topPath);
        });

        // And add another item, causing changes in the plan (if it all works out). There now must be 2 Review tasks
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), itemsPath), casePlan -> {
            casePlan.print();
//
//            i = 0;
//            System.out.println("New events: ");
//            testCase.getEventListener().getNewEvents().forEach(e -> {
//                System.out.println("Found "+(i++)+": "+e);
//            });
//            System.out.println("Done.");

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent(item1, e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent(item0);
            casePlan.getEvents().assertNoCaseFileEvent(item2);
            // Now there must be one more review task
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active);

            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem(topPath).assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent(topPath);
        });

        // And add another item, causing changes in the plan (if it all works out). There should not be any new Review tasks.
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), itemsPath), casePlan -> {

            // Now the entry criterion of the last task has been met, and no more repetition, so no new tasks
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active).assertNoMoreRepetition();

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent(item2, e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent(item0);
            casePlan.getEvents().assertNoCaseFileEvent(item1);
            casePlan.getEvents().assertNoCaseFileEvent(item3);
            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem(topPath).assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent(topPath);
        });

        // And add another item, should not lead to new changes
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), itemsPath), casePlan -> {

            // Now there must be still 3 active review tasks
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active).assertNoMoreRepetition();

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent(item3, e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent(item0);
            casePlan.getEvents().assertNoCaseFileEvent(item1);
            casePlan.getEvents().assertNoCaseFileEvent(item2);
            casePlan.getEvents().assertNoCaseFileEvent(item4);
            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem(topPath).assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent(topPath);
        });

        // And add another item, causing changes in the plan (if it all works out)
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), itemsPath), casePlan -> {
            casePlan.print();

            // Now there must be still 3 active review tasks
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active);

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent(item4, e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent(item0);
            casePlan.getEvents().assertNoCaseFileEvent(item1);
            casePlan.getEvents().assertNoCaseFileEvent(item2);
            casePlan.getEvents().assertNoCaseFileEvent(item3);
            casePlan.getEvents().assertNoCaseFileEvent(item5);
            // We should still have the former "TopCase" based CaseFileEvent ...
            casePlan.assertCaseFileItem(topPath).assertValue(topCaseObject);
            //  ... but there should not be any new events on it that come out of the action.
            casePlan.getEvents().assertNoCaseFileEvent(topPath);
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
        itemObject.plus("item", "Help, doe geen review");
        itemObject.plus("role", "expert");
        itemArray.add(itemObject.cloneValueNode()); // [0]
        itemArray.add(itemObject.cloneValueNode()); // [1]

        // Creating 2 items in the array should trigger 2 new Review tasks
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, topCaseObject.cloneValueNode(), topPath), casePlan -> {
            casePlan.print();
            // There must be 2 review tasks
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active);

            casePlan.assertCaseFileItem(item1).assertValue(itemObject);

            // First command should result in 2 array items under TopCase
            casePlan.assertCaseFileItem(topPath).assertValue(topCaseObject);
            casePlan.assertCaseFileItem(item0).assertValue(itemObject);
            casePlan.assertCaseFileItem(item1).assertValue(itemObject);
            casePlan.getEvents().assertCaseFileEvent(topPath, e -> e.getValue().equals(topCaseObject));
            casePlan.getEvents().assertCaseFileEvent(item0, e -> e.getValue().equals(itemObject));
            casePlan.getEvents().assertCaseFileEvent(item1, e -> e.getValue().equals(itemObject));
            // But nothing yet on the next item
            casePlan.getEvents().assertNoCaseFileEvent(item2);
        });

        // Add another item. There should not be a new Review task, and the repetition of the second Review task must be false.
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), itemsPath), casePlan -> {
            casePlan.print();

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent(item2, e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent(topPath);
            casePlan.getEvents().assertNoCaseFileEvent(item0);
            casePlan.getEvents().assertNoCaseFileEvent(item1);
            casePlan.getEvents().assertNoCaseFileEvent(item3);

            // There must be 2 review tasks, one active and one available, both repeating.
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active).assertNoMoreRepetition();
        });

        // And yet another item
        testCase.addStep(new CreateCaseFileItem(testUser, caseInstanceId, itemObject.cloneValueNode(), itemsPath), casePlan -> {
            casePlan.print();
            // There must be 2 review tasks, one active and one available.
            casePlan.assertPlanItems("Review").assertSize(2).assertStates(State.Active).assertNoMoreRepetition();

            // One more item ...
            casePlan.getEvents().assertCaseFileEvent(item3, e -> e.getValue().equals(itemObject));
            // But nothing on the previous, and not yet the next
            casePlan.getEvents().assertNoCaseFileEvent(topPath);
            casePlan.getEvents().assertNoCaseFileEvent(item0);
            casePlan.getEvents().assertNoCaseFileEvent(item1);
            casePlan.getEvents().assertNoCaseFileEvent(item2);
            casePlan.getEvents().assertNoCaseFileEvent(item4);

            // There should be total of 0 .. 3 events
            casePlan.assertCaseFileItem(item0).assertValue(itemObject);
            casePlan.assertCaseFileItem(item1).assertValue(itemObject);
            casePlan.assertCaseFileItem(item2).assertValue(itemObject);
            casePlan.assertCaseFileItem(item3).assertValue(itemObject);

        });

        testCase.runTest();
    }
}
