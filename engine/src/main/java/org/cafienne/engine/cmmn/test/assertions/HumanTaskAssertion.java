/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.engine.cmmn.test.assertions;

import org.cafienne.engine.cmmn.test.CaseTestCommand;
import org.cafienne.engine.humantask.actorapi.event.*;
import org.cafienne.engine.humantask.actorapi.response.HumanTaskValidationResponse;
import org.cafienne.engine.humantask.instance.TaskState;
import org.cafienne.json.ValueMap;

import java.time.Instant;

public class HumanTaskAssertion extends CaseAssertion {
//    private final String identifier;
//
//    public HumanTaskAssertion(CaseTestCommand testCommand, PlanItemCreated pic) {
//        super(testCommand);
//        this.identifier = pic.getPlanItemId();
//    }

//    HumanTaskAssertion assertType(Class c) {
//        filter("A HumanTask for identifier "+identifier+" has not given any events yet", e -> true);
//        return this;
//    }
////
//    void filter(String msg, EventFilter<HumanTaskTransitioned> filter) {
//        System.out.println("Searchign for HumanTaskTransitioned events");
//        testCommand.getEventListener().waitUntil(msg, HumanTaskTransitioned.class, e -> {
//            if (e.getTaskId().equals(testCommand.getActorId())) {
//                return filter.matches(e);
//            }
//            return false;
//        });
//    }

    public HumanTaskAssertion(CaseAssertion assertion) {
        this(assertion.getTestCommand());
    }

    public HumanTaskAssertion(CaseTestCommand testCommand) {
        super(testCommand);
    }

    public HumanTaskValidationResponse getValidationResponse() {
        return (HumanTaskValidationResponse) this.testCommand.getActualResponse();
    }

    /**
     * Wait for the task to emit a HumanTaskOutputSaved event with the expected output
     *
     * @return
     */
    public HumanTaskAssertion assertTaskOutput(ValueMap expectedOutput) {
        getEvents().filter(HumanTaskOutputSaved.class).filter(e -> e.getTaskOutput().equals(expectedOutput));
        return this;
    }

    /**
     * Asserts task expectedState
     *
     * @param expectedState
     * @return
     */
    public HumanTaskAssertion assertTaskState(TaskState expectedState) {
        getEvents().filter(HumanTaskTransitioned.class).filter(e -> e.getCurrentState().equals(expectedState));
        return this;
    }

    /**
     * Asserts task assignee
     *
     * @param expectedAssignee
     * @return
     */
    public HumanTaskAssertion assertAssignee(String expectedAssignee) {
        getEvents().filter(HumanTaskAssigned.class).filter(e -> e.assignee.equals(expectedAssignee));
        return this;
    }

    /**
     * Asserts task owner
     *
     * @param expectedOwner
     * @return
     */
    public HumanTaskAssertion assertOwner(String expectedOwner) {
        getEvents().filter(HumanTaskOwnerChanged.class).filter(e -> e.owner.equals(expectedOwner));
        return this;
    }

    /**
     * Asserts task due date
     *
     * @param expectedDueDate
     * @return
     */
    public HumanTaskAssertion assertDueDate(Instant expectedDueDate) {
        getEvents().filter(HumanTaskDueDateFilled.class).filter(e -> e.dueDate.equals(expectedDueDate));
        return this;
    }

    public HumanTaskAssertion assertTaskCompleted() {
        getEvents().filter(HumanTaskCompleted.class).assertNotEmpty();
        return this;
    }
}
