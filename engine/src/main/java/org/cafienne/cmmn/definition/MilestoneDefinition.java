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

package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.*;
import org.w3c.dom.Element;

public class MilestoneDefinition extends PlanItemDefinitionDefinition {
    public MilestoneDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    public PlanItemType getItemType() {
        return PlanItemType.Milestone;
    }

    @Override
    public Milestone createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        return new Milestone(id, index, itemDefinition, this, stage);
    }

    @Override
    public Transition getEntryTransition() {
        return Transition.Occur;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::samePlanItemDefinitionDefinition);
    }
}
