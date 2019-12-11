/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.cmmn.instance;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.akka.event.PlanItemCreated;
import org.cafienne.akka.actor.serialization.AkkaSerializable;
import org.cafienne.cmmn.instance.casefile.LongValue;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

public abstract class PlanItemEvent extends CaseInstanceEvent implements AkkaSerializable {
    private final static Logger logger = LoggerFactory.getLogger(PlanItemEvent.class);

    public final String planItemId;
    public final String type;
    public final int seqNo;
    public final int index;

    public enum Fields {
        planItemId, type, planitem, seqNo, index
    }

    protected PlanItemEvent(PlanItem planItem) {
        super(planItem.getCaseInstance());
        this.planItemId = planItem.getId();
        this.seqNo = planItem.getNextEventNumber();
        this.type = planItem.getType();
        this.index = planItem.getIndex();
    }

    protected PlanItemEvent(ValueMap json) {
        super(json);
        this.planItemId = readField(json, Fields.planItemId);
        this.type = readField(json, Fields.type);
        ValueMap planItemJson = readMap(json, Fields.planitem);
        this.seqNo = ((Long) planItemJson.raw(Fields.seqNo)).intValue();
        this.index = ((Long)readField(planItemJson, Fields.index)).intValue();
    }

    public void writePlanItemEvent(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.planItemId, planItemId);
        writeField(generator, Fields.type, type);

        generator.writeFieldName(Fields.planitem.toString());
        generator.writeStartObject();

        generator.writeNumberField(Fields.seqNo.toString(), seqNo);
        generator.writeNumberField(Fields.index.toString(), index);
        generator.writeEndObject();
    }

    protected void recoverEvent(Case caseInstance) {
        throw new RuntimeException("This method may  not be invoekd");
    }

    @Override
    final public void recover(Case caseInstance) {
        if (this instanceof PlanItemCreated) {
            this.recoverEvent(caseInstance);
        }
        PlanItem planItem = caseInstance.getPlanItemById(getPlanItemId());
        if (planItem == null) {
            logger.error("MAJOR ERROR: Cannot recover plan item transition for plan item with id " + getPlanItemId() + ", because the plan item cannot be found");
            return;
        }
        planItem.recover(this);
        this.recoverPlanItemEvent(planItem);
    }

    abstract protected void recoverPlanItemEvent(PlanItem planItem);

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