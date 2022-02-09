/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.cmmn.instance.CasePlan;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.test.CaseTestCommand;
import org.cafienne.cmmn.test.filter.EventFilter;

import java.util.stream.Stream;

public class StageAssertion extends PlanItemAssertion {

    StageAssertion(CaseTestCommand command, PlanItemCreated event) {
        super(command, event);
        if (event != null) {
            assertType(new Class[]{Stage.class, CasePlan.class});
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
        EventFilter<PlanItemCreated> filter = e -> e.getStageId().equals(this.getId()) && (e.getPlanItemId().equals(identifier) || e.getPlanItemName().equals(identifier));
        return pea.filter(filter).getEvents().stream();
    }
}
