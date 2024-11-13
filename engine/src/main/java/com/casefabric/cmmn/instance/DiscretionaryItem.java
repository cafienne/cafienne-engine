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

package com.casefabric.cmmn.instance;

import com.casefabric.cmmn.definition.DiscretionaryItemDefinition;
import com.casefabric.cmmn.definition.team.CaseRoleDefinition;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class DiscretionaryItem extends CMMNElement<DiscretionaryItemDefinition> {
    private final PlanItem<?> containingPlanItem;
    private final Stage<?> containingStage;
    
    /**
     * Creates a new discretionary item for the specified definition inside the parent (i.e., Task or Stage)
     * @param parentPlanItem
     * @param definition
     */
    public DiscretionaryItem(PlanItem<?> parentPlanItem, DiscretionaryItemDefinition definition) {
        super(parentPlanItem, definition);
        this.containingPlanItem = parentPlanItem;
        this.containingStage = containingPlanItem instanceof Stage<?> ? (Stage<?>) containingPlanItem : containingPlanItem.getStage();
    }
    
    /**
     * Determines whether the plan item is currently allowed (i.e., the parent plan item must have the proper state), and it also must be still applicable.
     * @return
     */
    public boolean isPlannable() {
        boolean isAllowed = getDefinition().isPlanningAllowed(containingPlanItem);
        boolean isApplicable = getDefinition().isApplicable(containingPlanItem);
        return isAllowed && isApplicable;
    }

    public Collection<CaseRoleDefinition> getAuthorizedRoles() {
        return getDefinition().getAuthorizedRoles();
    }

    /**
     * Returns whether or not the current user is allowed to add this discretionary item to the plan.
     * @return
     */
    public boolean isAuthorized() {
        return getCaseInstance().getCurrentTeamMember().hasRoles(getAuthorizedRoles());
    }

    /**
     * Adds the discretionary item to the plan, having the specified identifier
     * @param planItemId
     * @return
     */
    public void plan(String planItemId) {
        containingStage.planChild(this, planItemId);
    }

    public ValueMap asJson() {
        DiscretionaryItemDefinition definition = getDefinition();
        String name = definition.getName();
        String definitionId = definition.getId();
        PlanItemType type = definition.getPlanItemDefinition().getItemType();
        String parentName = containingPlanItem.getName();
        PlanItemType parentType = containingPlanItem.getType();
        String parentId = containingPlanItem.getId();
        return new ValueMap(Fields.name, name, Fields.definitionId, definitionId, Fields.type, type, Fields.parentName, parentName, Fields.parentType, parentType, Fields.parentId, parentId);
    }
}
