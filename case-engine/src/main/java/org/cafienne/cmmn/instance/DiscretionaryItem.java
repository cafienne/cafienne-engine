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

import java.util.Collection;
import java.util.Set;

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
        Collection<CaseRoleDefinition> authorizedRoles = getAuthorizedRoles();

        // When no roles have been defined, it is allowed.
        if (authorizedRoles.isEmpty()) {
            return true;
        }

        // Otherwise the current user must have the role in the case team.
        Set<CaseRoleDefinition> rolesOfCurrentUser = getCaseInstance().getCurrentTeamMember().getRoles();
        for (CaseRoleDefinition role : authorizedRoles) {
            if (rolesOfCurrentUser.contains(role)) {
                return true; // You're free to go
            }
        }

        return false;
    }

    /**
     * Adds the discretionary item to the plan, having the specified identifier
     * @param planItemId
     * @return
     */
    public void plan(String planItemId) {
        containingStage.planChild(this, planItemId);
    }

    /**
     * Returns the type of the runtime plan item, e.g. CaseTask, Stage, UserEvent, etc.
     *
     * @return
     */
    public String getType() {
        return getDefinition().getPlanItemDefinition().getType();
    }

    /**
     * Returns the id of the plan item that provides the context for planning this discretionary item.
     * @return
     */
    public String getParentId() {
        return containingPlanItem.getId();
    }
}
