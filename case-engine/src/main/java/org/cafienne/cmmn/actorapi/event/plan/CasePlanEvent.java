/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class CasePlanEvent<I extends PlanItem<?>> extends CaseEvent {
    private transient I planItem;

    private final String planItemId;
    private final String type;

    protected CasePlanEvent(I planItem) {
        super(planItem.getCaseInstance());
        this.planItemId = planItem.getId();
        this.type = planItem.getType();
        this.planItem = planItem;
    }

    protected CasePlanEvent(Case actor, String planItemId, String type) {
        super(actor);
        this.planItemId = planItemId;
        this.type = type;
        this.planItem = null;
    }

    protected CasePlanEvent(ValueMap json) {
        super(json);
        this.planItemId = readField(json, Fields.planItemId);
        this.type = readField(json, Fields.type);
        this.planItem = null;
    }

    protected void setPlanItem(I planItem) {
        this.planItem = planItem;
    }

    protected I getPlanItem() {
        return this.planItem;
    }

    @Override
    public void updateState(Case actor) {
        if (planItem == null) {
            planItem = actor.getPlanItemById(getPlanItemId());
            if (planItem == null) {
                logger.error("MAJOR ERROR in " + getClass().getSimpleName() + ": Cannot recover event, because plan item with id " + planItemId + " cannot be found; Case instance " + getActorId() +" with definition name '" + actor.getDefinition().getName() + "'");
                return;
            }
        }

        updateState(planItem);
    }

    public abstract void updateState(I item);

    public void writeCasePlanEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.planItemId, planItemId);
        writeField(generator, Fields.type, type);
    }

    /**
     * Returns type of plan item. Typically: HumanTask, Stage, Milestone, ProcessTask, CaseTask, etc.
     *
     * @return
     */
    public String getType() {
        return this.type;
    }

    public String getPlanItemId() {
        return planItemId;
    }
}