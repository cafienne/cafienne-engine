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

import org.cafienne.engine.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.engine.cmmn.instance.CasePlan;
import org.cafienne.engine.cmmn.instance.PlanItemType;
import org.cafienne.engine.cmmn.instance.Stage;
import org.cafienne.engine.cmmn.instance.State;
import org.cafienne.engine.cmmn.test.CaseTestCommand;
import org.cafienne.engine.cmmn.test.filter.EventFilter;

import java.util.stream.Stream;

public class StageAssertion extends PlanItemAssertion {

    StageAssertion(CaseTestCommand command, PlanItemCreated event) {
        super(command, event);
        if (event != null) {
            assertType(PlanItemType.Stage, PlanItemType.CasePlan);
        }
    }

    /**
     * Asserts that the stage has a substage with the expected identifier
     *
     * @param identifier
     * @return
     */
    public StageAssertion assertStage(String identifier) {
        return new StageAssertion(testCommand, getPlanItem("stage", identifier));
    }

    /**
     * Asserts that the stage has a task with the expected identifier
     *
     * @param identifier
     * @return
     */
    public TaskAssertion assertTask(String identifier) {
        return new TaskAssertion(testCommand, getPlanItem("task", identifier));
    }

    /**
     * Asserts that the stage has a human task with the expected identifier
     *
     * @param identifier
     * @return
     */
    public TaskAssertion assertHumanTask(String identifier) {
        return new TaskAssertion(testCommand, getPlanItem("task", identifier));
    }

    /**
     * Asserts that the stage has a plan item with the expected identifier
     *
     * @param identifier
     * @return
     */
    public PlanItemAssertion assertPlanItem(String identifier) {
        return new PlanItemAssertion(testCommand, getPlanItem("plan item", identifier));
    }

    /**
     * Asserts that the stage has a plan item with the expected identifier
     *
     * @param identifier
     * @return
     */
    public PlanItemAssertion assertPlanItem(String identifier, State expectedState) {
        return assertPlanItems(identifier, expectedState).assertSize(1).first();
    }

    /**
     * Returns a {@link PlanItemSetAssertion} for all plan items with the identifier.
     *
     * @param identifier
     * @return
     */
    public PlanItemSetAssertion assertPlanItems(String identifier) {
        PlanItemSetAssertion pisa = new PlanItemSetAssertion(identifier);
        getPlanItems(identifier).forEach(planItem -> {
            PlanItemAssertion pia = new PlanItemAssertion(testCommand, planItem);
            pisa.add(pia);
        });
        return pisa;
    }

    public PlanItemSetAssertion assertPlanItems(String identifier, State expectedState) {
        return assertPlanItems(identifier).filter(expectedState);
    }

    private PlanItemCreated getPlanItem(String errorMsg, String identifier) {
        PlanItemCreated planItem = getPlanItems(identifier).findFirst().orElse(null);
        if (planItem == null) {
            throw new AssertionError("The " + errorMsg + " '" + identifier + "' cannot be found in " + getName());
        }
//        System.out.println("Foudn planitem for identifier "+identifier +" with name "+planItem.getPlanItemName()+", and id: "+planItem.getPlanItemId());
        return planItem;
    }

    /**
     * This method is protected, so that it can be overridden in CaseAssertion, fetching ALL planitems of the case,
     * rather than just from the current stage.
     *
     * @param identifier
     * @return
     */
    protected Stream<PlanItemCreated> getPlanItems(String identifier) {
        PublishedEventsAssertion<PlanItemCreated> pea = testCommand.getEventListener().getEvents().filter(caseId).filter(PlanItemCreated.class);
        EventFilter<PlanItemCreated> filter = e -> e.stageId.equals(this.getId()) && (e.getPlanItemId().equals(identifier) || e.getPlanItemName().equals(identifier));
        return pea.filter(filter).getEvents().stream();
    }
}
