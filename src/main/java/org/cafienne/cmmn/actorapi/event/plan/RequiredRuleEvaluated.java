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

package org.cafienne.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class RequiredRuleEvaluated extends CasePlanEvent {
    private final boolean isRequired;

    public RequiredRuleEvaluated(PlanItem<?> planItem, boolean isRequired) {
        super(planItem);
        this.isRequired = isRequired;
    }

    public RequiredRuleEvaluated(ValueMap json) {
        super(json);
        this.isRequired = json.readBoolean(Fields.isRequired);
    }

    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + "[" + getName() + "/" + getPlanItemId() + "]: REQUIRED = " + isRequired;
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        planItem.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeCasePlanEvent(generator);
        writeField(generator, Fields.isRequired, this.isRequired);
    }
}
