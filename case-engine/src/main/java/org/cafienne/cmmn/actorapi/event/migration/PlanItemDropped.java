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
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class PlanItemDropped extends PlanItemEvent {
    public PlanItemDropped(PlanItem<?> item) {
        super(item);
    }

    public PlanItemDropped(ValueMap json) {
        super(json);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writePlanItemEvent(generator);
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        planItem.updateState(this);
    }
}
