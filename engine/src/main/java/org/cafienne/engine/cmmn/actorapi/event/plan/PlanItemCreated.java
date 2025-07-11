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
import org.cafienne.engine.cmmn.definition.ItemDefinition;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.Path;
import org.cafienne.engine.cmmn.instance.PlanItem;
import org.cafienne.engine.cmmn.instance.Stage;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.util.Guid;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class PlanItemCreated extends CasePlanEvent {
    public final String definitionId;

    public PlanItemCreated(Case caseInstance) {
        this(caseInstance, new Guid().toString(), "", new Path(""), caseInstance.getDefinition().getCasePlanModel(), 0);
    }

    public PlanItemCreated(Stage<?> stage, ItemDefinition definition, String planItemId, int index) {
        this(stage.getCaseInstance(), planItemId, stage.getId(), stage.getPath(), definition, index);
    }

    private PlanItemCreated(Case caseInstance, String planItemId, String parentStage, Path parentPath, ItemDefinition definition, int index) {
        super(caseInstance, planItemId, parentStage, new Path(parentPath, definition, index), definition.getPlanItemDefinition().getItemType(), null);
        this.definitionId = definition.getId();
    }

    public PlanItemCreated(ValueMap json) {
        super(json);
        this.definitionId = json.readString(Fields.definitionId, "");
        // Note: createdOn and planItemName are no longer persisted, since the parent class contains all info.
        //  However, older events may not carry it, so if parent class does not have it, then read it old style.
        //  This is done in getters on these fields. Former field createdBy is no longer required.
    }

    @Override
    public String getDescription() {
        return "PlanItemCreated [" + getType() + "-" + getPlanItemName() + "." + getIndex() + "/" + getPlanItemId() + "]" + (stageId.isEmpty() ? "" : " in stage " + stageId);
    }

    public Instant getCreatedOn() {
        return getTimestamp() == null ? rawJson().readInstant(Fields.createdOn) : getTimestamp();
    }

    public String getPlanItemName() {
        return path.isEmpty() ? rawJson().readString(Fields.name) : path.name; // Compatibility with older events that do not carry path;
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
        writeField(generator, Fields.definitionId, definitionId);
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        // Nothing to do here, since we overwrite updateState(Case actor).
    }
}
