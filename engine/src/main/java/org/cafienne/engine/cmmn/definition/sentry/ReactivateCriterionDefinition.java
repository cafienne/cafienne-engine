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

package org.cafienne.engine.cmmn.definition.sentry;

import org.cafienne.engine.cmmn.definition.*;
import org.cafienne.engine.cmmn.instance.Transition;
import org.w3c.dom.Element;

public class ReactivateCriterionDefinition extends CriterionDefinition {
    public ReactivateCriterionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    /**
     * Only after resolving sentries we know what transition we need to make
     */
    @Override
    protected void resolveReferences() {
        super.resolveReferences();

        PlanItemDefinitionDefinition parent = getParentType();
        if (parent == null) {
            getCaseDefinition().addReferenceError(getContextDescription() + "Found a reactivate criterion inside a " + getParentElement().getClass().getSimpleName() + ", but that type is not supported for reactivate criteria");
        } else {
            if (! (parent instanceof TaskDefinition || parent instanceof StageDefinition)) {
                getCaseDefinition().addReferenceError(getContextDescription() + "Cannot set a reactivate criterion on " + getParentElement().getName() +" because it is of type " + parent.getType());
            }
        }
    }

    private PlanItemDefinitionDefinition getParentType() {
        CMMNElementDefinition parent = getParentElement();
        if (parent instanceof PlanItemDefinition) {
            return ((PlanItemDefinition) parent).getPlanItemDefinition();
        } else if (parent instanceof DiscretionaryItemDefinition) {
            return ((DiscretionaryItemDefinition) parent).getPlanItemDefinition();
        } else {
            return null;
        }
    }

    @Override
    public Transition getTransition() {
        return Transition.Reactivate;
    }
}
