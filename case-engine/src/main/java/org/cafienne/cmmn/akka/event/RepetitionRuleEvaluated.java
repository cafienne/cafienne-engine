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
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.PlanItemEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

@Manifest
public class RepetitionRuleEvaluated extends PlanItemEvent {
    private final boolean isRepeating;

    private enum Fields {
        isRepeating
    }

    public RepetitionRuleEvaluated(PlanItem planItem) {
        super(planItem);
        this.isRepeating = planItem.repeats();
    }

    public RepetitionRuleEvaluated(ValueMap json) {
        super(json);
        this.isRepeating = readField(json, Fields.isRepeating);
    }

    public boolean isRepeating() {
        return isRepeating;
    }

    @Override
    public String toString() {
        return "Repetition rule outcome for " + getPlanItemId() + ": " + isRepeating();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writePlanItemEvent(generator);
        writeField(generator, Fields.isRepeating, this.isRepeating);
    }

    @Override
    protected void recoverPlanItemEvent(PlanItem planItem) {
        planItem.recover(this);
    }
}
