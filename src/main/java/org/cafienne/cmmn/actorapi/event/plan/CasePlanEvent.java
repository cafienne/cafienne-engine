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
import org.cafienne.cmmn.actorapi.event.CaseBaseEvent;
import org.cafienne.cmmn.instance.*;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class CasePlanEvent extends CaseBaseEvent {
    public final String planItemId;
    public final String stageId;
    public final Path path;
    public final PlanItemType type;
    private final int index;
    private transient PlanItem<?> planItem;

    private static String getStageId(PlanItem<?> planItem) {
        Stage<?> stage = planItem.getStage();
        return stage == null ? "" : stage.getId();
    }

    protected CasePlanEvent(PlanItem<?> planItem) {
        this(planItem.getCaseInstance(), planItem.getId(), getStageId(planItem), planItem.getPath(), planItem.getType(), planItem);
    }

    protected CasePlanEvent(Case actor, String planItemId, String stageId, Path path, PlanItemType type, PlanItem<?> planItem) {
        super(actor);
        this.planItemId = planItemId;
        this.stageId = stageId;
        this.path = path;
        this.type = type;
        this.index = readIndex(path, null);
        this.planItem = planItem;
    }

    protected CasePlanEvent(ValueMap json) {
        super(json);
        this.planItemId = json.readString(Fields.planItemId);
        // Stage id is promoted from only PlanItemCreated into each CasePlanEvent. Older events do not have it, and get an empty value.
        this.stageId = json.readString(Fields.stageId, "");
        this.path = json.readPath(Fields.path, "");
        this.type = json.readEnum(Fields.type, PlanItemType.class);
        this.planItem = null; // Not available in event reading, except inside recovery of an event.
        this.index = readIndex(path, json);
    }

    private int readIndex(Path path, ValueMap json) {
        if (path.isEmpty()) {
            // This is older version of events. Path is not available, but a separate 'planitem' json containing the index.
            // Furthermore, TaskEvent and TimerEvent are now also a PlanItemEvent.
            //  Older versions of those events do not have path and also not the plan item with index. We're providing -1 as the default value to recognize that.
            return json.with(Fields.planitem).readLong(Fields.index, -1L).intValue();
        } else {
            // Note, this code is quite particular. Path supports array style indices. If a plan item is _not_ repeating
            //  i.e., if it does _not_ have a repetitionrule defined, then -1 is stored in the path to prevent it
            //  from printing the square array brackets ([]). But ... inside the engine's CMMN logic, comparison is done
            //  on index == 0. Better option is to change the engine algoritm to use logic based on the "-1" information
            //  if that is available. This also means that path probably has to become a plan item constructor property.
            return Math.max(0, path.index);
//            return path.index;
        }
    }

    protected void setPlanItem(PlanItem<?> planItem) {
        this.planItem = planItem;
    }

    protected PlanItem<?> getPlanItem() {
        return this.planItem;
    }

    @Override
    public void updateState(Case actor) {
        if (planItem == null) {
            planItem = actor.getPlanItemById(getPlanItemId());
            if (planItem == null) {
                logger.error("MAJOR ERROR in " + getClass().getSimpleName() + ": Cannot recover event, because plan item with id " + planItemId + " cannot be found; Case instance " + getActorId() + " with definition name '" + actor.getDefinition().getName() + "'");
                return;
            }
        }
        updatePlanItemState(planItem);
    }

    abstract protected void updatePlanItemState(PlanItem<?> planItem);

    public void writeCasePlanEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.planItemId, planItemId);
        writeField(generator, Fields.stageId, stageId);
        writeField(generator, Fields.path, path);
        writeField(generator, Fields.type, type);
    }

    /**
     * Returns type of plan item. Typically: HumanTask, Stage, Milestone, ProcessTask, CaseTask, etc.
     *
     * @return
     */
    public PlanItemType getType() {
        return this.type;
    }

    public String getPlanItemId() {
        return planItemId;
    }

    protected String getName() {
        return getPlanItem() != null ? getPlanItem().getName() + "." + getIndex() : "PlanItem";
    }

    public int getIndex() {
        return index;
    }
}