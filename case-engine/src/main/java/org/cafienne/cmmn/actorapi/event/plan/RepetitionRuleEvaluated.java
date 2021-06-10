/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.serialization.Fields;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class RepetitionRuleEvaluated extends PlanItemEvent {
    private final boolean isRepeating;

    public RepetitionRuleEvaluated(PlanItem planItem, boolean repeats) {
        super(planItem);
        this.isRepeating = repeats;
    }

    public RepetitionRuleEvaluated(ValueMap json) {
        super(json);
        this.isRepeating = readField(json, Fields.isRepeating);
    }

    public boolean isRepeating() {
        return isRepeating;
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + "[" + getName() + "/" + getPlanItemId() + "]: REPEAT = " + isRepeating;
    }

    @Override
    protected void updatePlanItemState(PlanItem planItem) {
        planItem.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writePlanItemEvent(generator);
        writeField(generator, Fields.isRepeating, this.isRepeating);
    }
}
