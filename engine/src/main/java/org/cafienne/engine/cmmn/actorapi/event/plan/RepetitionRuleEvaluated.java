/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.engine.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.engine.cmmn.instance.PlanItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class RepetitionRuleEvaluated extends CasePlanEvent {
    private final boolean isRepeating;

    public RepetitionRuleEvaluated(PlanItem<?> planItem, boolean repeats) {
        super(planItem);
        this.isRepeating = repeats;
    }

    public RepetitionRuleEvaluated(ValueMap json) {
        super(json);
        this.isRepeating = json.readBoolean(Fields.isRepeating);
    }

    public boolean isRepeating() {
        return isRepeating;
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + "[" + getName() + "/" + getPlanItemId() + "]: REPEAT = " + isRepeating;
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        planItem.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeCasePlanEvent(generator);
        writeField(generator, Fields.isRepeating, this.isRepeating);
    }
}
