/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Path;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.util.Guid;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class PlanItemCreated extends CasePlanEvent {
    public final Instant createdOn;
    public final String createdBy;
    public final String planItemName;
    public final String definitionId;

    private static Path createPath(Path parent, ItemDefinition definition, int index) {
        String parentPath = parent.isEmpty() ? "" : parent + "/";
        boolean mayRepeat = !definition.getPlanItemControl().getRepetitionRule().isDefault();
        String myPath = definition.getName() + (mayRepeat ? "[" + index + "]" : "");
        return new Path(parentPath + myPath);
    }

    public PlanItemCreated(Case caseInstance) {
        this(caseInstance, new Guid().toString(), "", new Path(""), caseInstance.getDefinition().getCasePlanModel(), 0);
    }

    public PlanItemCreated(Stage<?> stage, ItemDefinition definition, String planItemId, int index) {
        this(stage.getCaseInstance(), planItemId, stage.getId(), stage.getPath(), definition, index);
    }

    private PlanItemCreated(Case caseInstance, String planItemId, String parentStage, Path parentPath, ItemDefinition definition, int index) {
        super(caseInstance, planItemId, parentStage, createPath(parentPath, definition, index), definition.getPlanItemDefinition().getItemType(), index, 0, null);
        this.createdOn = caseInstance.getTransactionTimestamp();
        this.createdBy = caseInstance.getCurrentUser().id();
        this.planItemName = definition.getName();
        this.definitionId = definition.getId();
    }

    public PlanItemCreated(ValueMap json) {
        super(json);
        this.createdOn = json.readInstant(Fields.createdOn);
        this.createdBy = json.readString(Fields.createdBy);
        this.planItemName = json.readString(Fields.name);
        this.definitionId = json.readString(Fields.definitionId, "");
    }

    @Override
    public String getDescription() {
        return "PlanItemCreated [" + getType() + "-" + getPlanItemName() + "." + getIndex() + "/" + getPlanItemId() + "]" + (stageId.isEmpty() ? "" : " in stage " + stageId);
    }

    public String getPlanItemName() {
        return planItemName;
    }

    public PlanItem<?> getCreatedPlanItem() {
        return getPlanItem();
    }

    @Override
    public void updateState(Case actor) {
        PlanItem<?> item = actor.add(this);
        setPlanItem(item);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCasePlanEvent(generator);
        writeField(generator, Fields.name, planItemName);
        writeField(generator, Fields.definitionId, definitionId);
        writeField(generator, Fields.createdOn, createdOn);
        writeField(generator, Fields.createdBy, createdBy);
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        // Nothing to do here, since we overwrite updateState(Case actor).
    }
}
