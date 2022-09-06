/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.CaseBaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class CasePlanEvent extends CaseBaseEvent {
    public final int seqNo;
    public final int index;
    private transient PlanItem<?> planItem;

    private final String planItemId;
    private final String type;

    protected CasePlanEvent(PlanItem<?> planItem) {
        this(planItem.getCaseInstance(), planItem.getId(), planItem.getType(), planItem.getIndex(), planItem.getNextEventNumber(), planItem);
    }

    protected CasePlanEvent(Case actor, String planItemId, String type, int index, int seqNo, PlanItem<?> planItem) {
        super(actor);
        this.planItemId = planItemId;
        this.type = type;
        this.seqNo = seqNo;
        this.index = index;
        this.planItem = planItem;
    }

    protected CasePlanEvent(ValueMap json) {
        super(json);
        this.planItemId = json.readString(Fields.planItemId);
        this.type = json.readString(Fields.type);
        this.planItem = null;

        // TaskEvent and TimerEvent are now also a PlanItemEvent.
        //  Older versions of those events do not have a seqNo and index. We're providing -1 as the default value to recognize that.
        ValueMap planItemJson = json.readMap(Fields.planitem);
        this.seqNo = planItemJson.readLong(Fields.seqNo, -1L).intValue();
        this.index = planItemJson.readLong(Fields.index, -1L).intValue();
    }

    protected void setPlanItem(PlanItem planItem) {
        this.planItem = planItem;
    }

    protected PlanItem getPlanItem() {
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
        planItem.updateSequenceNumber(this);
        updatePlanItemState(planItem);
    }

    abstract protected void updatePlanItemState(PlanItem<?> planItem);

    public void writeCasePlanEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.planItemId, planItemId);
        writeField(generator, Fields.type, type);
        // Make a special planitem section
        generator.writeFieldName(Fields.planitem.toString());
        generator.writeStartObject();
        generator.writeNumberField(Fields.seqNo.toString(), seqNo);
        generator.writeNumberField(Fields.index.toString(), index);
        generator.writeEndObject();
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

    protected String getName() {
        return getPlanItem() != null ? getPlanItem().getName() + "." + getIndex() : "PlanItem";
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