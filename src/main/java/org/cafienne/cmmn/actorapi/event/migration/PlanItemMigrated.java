/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemEvent;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class PlanItemMigrated extends PlanItemEvent {
    public final String planItemName;
    public final String definitionId;

    public PlanItemMigrated(PlanItem<?> item) {
        super(item);
        this.planItemName = item.getName();
        this.definitionId = item.getItemDefinition().getId();
    }

    public PlanItemMigrated(ValueMap json) {
        super(json);
        this.planItemName = json.readString(Fields.name);
        this.definitionId = json.readString(Fields.definitionId, "");
    }

    @Override
    public String getDescription() {
        return "PlanItemCreated [" + getType() + "-" + getPlanItemName() + "." + getIndex() + "/" + getPlanItemId() + "]";
    }

    public String getPlanItemName() {
        return planItemName;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writePlanItemEvent(generator);
        writeField(generator, Fields.name, planItemName);
        writeField(generator, Fields.definitionId, definitionId);
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        // Nothing to do here, since we overwrite updateState(Case actor).
    }
}
