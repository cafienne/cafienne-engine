/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

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
