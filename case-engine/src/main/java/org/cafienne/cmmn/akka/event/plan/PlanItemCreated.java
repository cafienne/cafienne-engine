/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.util.Guid;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class PlanItemCreated extends PlanItemEvent {
    public final Instant createdOn;
    public final String createdBy;
    public final String planItemName;
    public final String stageId;

    public PlanItemCreated(Case caseInstance) {
        this(caseInstance, new Guid().toString(), caseInstance.getDefinition().getCasePlanModel().getName(), null, caseInstance.getDefinition().getCasePlanModel(), 0);
    }

    public PlanItemCreated(Stage stage, ItemDefinition definition, String planItemId, int index) {
        this(stage.getCaseInstance(), planItemId, definition.getName(), stage, definition, index);
    }

    private PlanItemCreated(Case caseInstance, String planItemId, String name, Stage stage, ItemDefinition definition, int index) {
        super(caseInstance, planItemId, definition.getPlanItemDefinition().getType(), index, 0);
        this.createdOn = caseInstance.getTransactionTimestamp();
        this.createdBy = caseInstance.getCurrentUser().id();
        this.planItemName = name;
        this.stageId = stage == null ? "" : stage.getId();
    }

    public PlanItemCreated(ValueMap json) {
        super(json);
        this.createdOn = readInstant(json, Fields.createdOn);
        this.createdBy = readField(json, Fields.createdBy);
        this.planItemName = readField(json, Fields.name);
        this.stageId = readField(json, Fields.stageId);
    }

    @Override
    public String getDescription() {
        return "PlanItemCreated [" + getType() + "-" + getPlanItemName() + "." + getIndex() + "/" + getPlanItemId() + "]" + (getStageId().isEmpty() ? "" : " in stage " + getStageId());
    }

    public String getPlanItemName() {
        return planItemName;
    }

    public String getStageId() {
        return stageId;
    }

    private transient PlanItem planItem;

    public PlanItem getCreatedPlanItem() {
        return planItem;
    }

    @Override
    public void updateState(Case actor) {
        planItem = actor.add(this);
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
    protected void updatePlanItemState(PlanItem planItem) {
        // Nothing to do here, since we overwrite updateState(Case actor).
    }
}
