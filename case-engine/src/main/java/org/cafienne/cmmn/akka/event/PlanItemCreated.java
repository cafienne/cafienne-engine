/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.PlanItemEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class PlanItemCreated extends PlanItemEvent {
    private final static Logger logger = LoggerFactory.getLogger(PlanItemCreated.class);

    public final Instant createdOn;
    public final String createdBy;
    public final String planItemName;
    public final String stageId;

    public enum Fields {
        name, stageId, createdOn, createdBy
    }

    public PlanItemCreated(PlanItem planItem) {
        super(planItem);
        this.createdOn = Instant.now();
        this.createdBy = planItem.getCaseInstance().getCurrentUser().id();
        this.planItemName = planItem.getName();
        if (planItem.getStage() == null) stageId = "";
        else stageId = planItem.getStage().getId();
    }

    public PlanItemCreated(ValueMap json) {
        super(json);
        this.createdOn = readInstant(json, Fields.createdOn);
        this.createdBy = readField(json, Fields.createdBy);
        this.planItemName = readField(json, Fields.name);
        this.stageId = readField(json, Fields.stageId);
    }

    @Override
    public String toString() {
        return "Created plan item of type "+getType()+" with name " + getPlanItemName() + " and id " + getPlanItemId() + (getStageId().isEmpty() ? "" : " in stage " + getStageId());
    }

    public String getPlanItemName() {
        return planItemName;
    }

    public String getStageId() {
        return stageId;
    }

    @Override
    protected void recoverEvent(Case caseInstance) {
        caseInstance.recoverPlanItem(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writePlanItemEvent(generator);
        writeField(generator, Fields.name, planItemName);
        writeField(generator, Fields.createdOn, createdOn);
        writeField(generator, Fields.createdBy, createdBy);
        writeField(generator, Fields.stageId, stageId);
    }

    @Override
    protected void recoverPlanItemEvent(PlanItem planItem) {
        // Nothing to do here.
    }
}
