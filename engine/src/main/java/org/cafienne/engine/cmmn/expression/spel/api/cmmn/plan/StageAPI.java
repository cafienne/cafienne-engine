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

package org.cafienne.engine.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.engine.cmmn.definition.ItemDefinition;
import org.cafienne.engine.cmmn.instance.*;
import org.cafienne.engine.cmmn.instance.Task;

import java.util.*;

/**
 *
 */
public class StageAPI extends PlanItemAPI<Stage<?>> {

    protected StageAPI(CaseAPI caseAPI, Stage<?> stage, StageAPI parent) {
        super(caseAPI, stage, parent);
        Collection<PlanItem<?>> items = stage.getPlanItems();
        Map<String, Object> itemAccessorsByName = new HashMap<>();
        for (PlanItem<?> item : items) {
            PlanItemAPI<?> childContext = createPlanItemContext(item);
            ItemDefinition itemDefinition = item.getItemDefinition();
            if (itemDefinition.getPlanItemControl().getRepetitionRule().isDefault()) {
                itemAccessorsByName.put(itemDefinition.getName(), childContext);
            } else {
                List list = (List) itemAccessorsByName.getOrDefault(itemDefinition.getName(), new ArrayList<PlanItemAPI<?>>());
                list.add(childContext);
                itemAccessorsByName.put(itemDefinition.getName(), list);
            }
        }
        itemAccessorsByName.forEach((name, item) -> addPropertyReader(name, () -> item));
        addPropertyReader("items", itemAccessorsByName::values);
        addDeprecatedReader("planItems", "items", itemAccessorsByName::values);
    }

    private PlanItemAPI<?> createPlanItemContext(PlanItem<?> item) {
        if (item instanceof Stage) {
            return new StageAPI(caseAPI, (Stage<?>) item, this);
        } else if (item instanceof Task) {
            return new TaskAPI(caseAPI, (Task<?>) item, this);
        } else if (item instanceof Milestone) {
            return new MilestoneAPI(caseAPI, (Milestone) item, this);
        } else if (item instanceof TimerEvent) {
            return new TimerEventAPI(caseAPI, (TimerEvent) item, this);
        } else {
            // Hmmm... a not yet supported type of plan item? That's ok.
            return new PlanItemAPI<>(caseAPI, item, this);
        }
    }

    @Override
    public String getName() {
        return "stage";
    }

}
