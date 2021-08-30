/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class PlanItemEvent extends CaseEvent {
    private final static Logger logger = LoggerFactory.getLogger(PlanItemEvent.class);

    protected final transient PlanItem<?> planItem;

    public final String planItemId;
    public final String type;
    public final int seqNo;
    public final int index;

    protected PlanItemEvent(PlanItem<?> planItem) {
        this(planItem.getCaseInstance(), planItem.getId(), planItem.getType(), planItem.getIndex(), planItem.getNextEventNumber(), planItem);
    }

    protected PlanItemEvent(Case actor, String planItemId, String type, int index, int seqNo) {
        this(actor, planItemId, type, index, seqNo, null);
    }

    protected PlanItemEvent(Case actor, String planItemId, String type, int index, int seqNo, PlanItem<?> planItem) {
        super(actor);
        this.planItemId = planItemId;
        this.seqNo = seqNo;
        this.type = type;
        this.index = index;
        this.planItem = planItem;
    }

    protected PlanItemEvent(ValueMap json) {
        super(json);
        this.planItemId = readField(json, Fields.planItemId);
        this.type = readField(json, Fields.type);
        ValueMap planItemJson = readMap(json, Fields.planitem);
        this.seqNo = ((Long) planItemJson.raw(Fields.seqNo)).intValue();
        this.index = ((Long)readField(planItemJson, Fields.index)).intValue();
        this.planItem = null;
    }

    public void writePlanItemEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.planItemId, planItemId);
        writeField(generator, Fields.type, type);

        generator.writeFieldName(Fields.planitem.toString());
        generator.writeStartObject();

        generator.writeNumberField(Fields.seqNo.toString(), seqNo);
        generator.writeNumberField(Fields.index.toString(), index);
        generator.writeEndObject();
    }

    @Override
    public void updateState(Case actor) {
        PlanItem<?> planItem = actor.getPlanItemById(planItemId);
        if (planItem == null) {
            // Ouch!
            logger.error("Error while updating state from event " + getClass().getSimpleName()+": cannot find plan item with id " + getPlanItemId() + " in case " + actor);
            return;
        }
        planItem.updateSequenceNumber(this);
        updatePlanItemState(planItem);
    }

    abstract protected void updatePlanItemState(PlanItem<?> planItem);

    protected String getName() {
        return planItem != null ? planItem.getName() + "." + getIndex() : "PlanItem";
    }

    /**
     * Returns type of task, taken from plan item. Typically HumanTask, ProcessTask or CaseTask.
     * @return
     */
    public String getType() {
        return this.type;
    }

    public String getPlanItemId() {
        return planItemId;
    }

    public String getId() {
        return getPlanItemId() + "_" + seqNo;
    }

    public int getSequenceNumber() {
        return seqNo;
    }

    public int getIndex() {
        return index;
    }
}