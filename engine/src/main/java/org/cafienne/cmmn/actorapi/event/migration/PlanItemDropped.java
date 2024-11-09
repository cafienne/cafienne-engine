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

package org.cafienne.cmmn.actorapi.event.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.plan.CasePlanEvent;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class PlanItemDropped extends CasePlanEvent {
    public PlanItemDropped(PlanItem<?> item) {
        super(item);
    }

    public PlanItemDropped(ValueMap json) {
        super(json);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCasePlanEvent(generator);
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        planItem.updateState(this);
    }
}
