/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import java.util.Collection;
import java.util.Set;

import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.definition.UserEventDefinition;
import org.cafienne.cmmn.user.CaseTeamMember;
import org.w3c.dom.Element;

public class UserEvent extends PlanItemDefinitionInstance<UserEventDefinition> {
    public UserEvent(PlanItem planItem, UserEventDefinition definition) {
        super(planItem, definition, StateMachine.EventMilestone);
    }

    public Collection<CaseRoleDefinition> getAuthorizedRoles() {
        return getDefinition().getAuthorizedRoles();
    }

    @Override
    protected boolean isTransitionAllowed(Transition transition) {
        if (transition != Transition.Occur) { // Only validating whether current user can make this event 'Occur'
            return true;
        }

        Collection<CaseRoleDefinition> authorizedRoles = getAuthorizedRoles();
        if (authorizedRoles.isEmpty()) { // No roles defined, so it is allowed.
            return true;
        }
        
        CaseTeamMember currentUser = getCaseInstance().getCurrentTeamMember();
        if (currentUser == null) { // No user found, but there must be one with a role
            throw new TransitionDeniedException("You do not have the permission to raise the event " + getPlanItem().getName());
        }
        
        // Now fetch roles of current user within this case and see if there is one that matches one of the authorized roles
        Set<CaseRoleDefinition> rolesOfCurrentUser = currentUser.getRoles();
        for (CaseRoleDefinition role : authorizedRoles) {
            if (rolesOfCurrentUser.contains(role)) {
                return true; // You're free to go
            }
        }
        // Apparently no matching role was found.s
        throw new TransitionDeniedException("You do not have the permission to raise the event " + getPlanItem().getName());
    }

    @Override
    protected void dumpMemoryStateToXML(Element planItemXML) {
        super.dumpMemoryStateToXML(planItemXML);
        Collection<CaseRoleDefinition> roles = getAuthorizedRoles();
        for (CaseRoleDefinition role : roles) {
            String roleName = role.getName();
            Element roleElement = planItemXML.getOwnerDocument().createElement("Role");
            planItemXML.appendChild(roleElement);
            roleElement.setAttribute("name", roleName);
        }
    }
}
