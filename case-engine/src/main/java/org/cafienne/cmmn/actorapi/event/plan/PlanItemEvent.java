/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class PlanItemEvent extends CasePlanEvent<PlanItem<?>> {
    public final int seqNo;
    public final int index;

    protected PlanItemEvent(PlanItem<?> planItem) {
        this(planItem.getCaseInstance(), planItem.getId(), planItem.getType(), planItem.getIndex(), planItem.getNextEventNumber(), planItem);
    }

    protected PlanItemEvent(Case actor, String planItemId, String type, int index, int seqNo) {
        this(actor, planItemId, type, index, seqNo, null);
    }

    protected PlanItemEvent(Case actor, String planItemId, String type, int index, int seqNo, PlanItem<?> planItem) {
        super(actor, planItemId, type);
        this.seqNo = seqNo;
        this.index = index;
    }

    protected PlanItemEvent(ValueMap json) {
        super(json);
        ValueMap planItemJson = readMap(json, Fields.planitem);
        this.seqNo = ((Long) planItemJson.raw(Fields.seqNo)).intValue();
        this.index = ((Long)readField(planItemJson, Fields.index)).intValue();
    }

    public void writePlanItemEvent(JsonGenerator generator) throws IOException {
        super.writeCasePlanEvent(generator);
        generator.writeFieldName(Fields.planitem.toString());
        generator.writeStartObject();
        generator.writeNumberField(Fields.seqNo.toString(), seqNo);
        generator.writeNumberField(Fields.index.toString(), index);
        generator.writeEndObject();
    }

    @Override
    public void updateState(PlanItem<?> planItem) {
        planItem.updateSequenceNumber(this);
        updatePlanItemState(planItem);
    }

    abstract protected void updatePlanItemState(PlanItem<?> planItem);

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