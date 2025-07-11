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

package org.cafienne.engine.cmmn.instance;

import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.engine.cmmn.actorapi.command.team.CurrentMember;
import org.cafienne.engine.cmmn.definition.ItemDefinition;
import org.cafienne.engine.cmmn.definition.UserEventDefinition;
import org.cafienne.engine.cmmn.definition.team.CaseRoleDefinition;
import org.w3c.dom.Element;

import java.util.Collection;

public class UserEvent extends EventListener<UserEventDefinition> {
    public UserEvent(String id, int index, ItemDefinition itemDefinition, UserEventDefinition definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage);
    }

    public Collection<CaseRoleDefinition> getAuthorizedRoles() {
        return getDefinition().getAuthorizedRoles();
    }

    @Override
    public void validateTransition(Transition transition) {
        super.validateTransition(transition);
        if (transition != Transition.Occur) { // Only validating whether current user can make this event 'Occur'
            return;
        }

        CurrentMember currentUser = getCaseInstance().getCurrentTeamMember();
        if (!currentUser.hasRoles(getAuthorizedRoles())) {
            // Apparently no matching role was found.
            throw new AuthorizationException("User '" + currentUser.userId() + "' does not have the permission to raise the event " + getName());
        }
    }

    @Override
    protected void dumpImplementationToXML(Element planItemXML) {
        super.dumpImplementationToXML(planItemXML);
        Collection<CaseRoleDefinition> roles = getAuthorizedRoles();
        for (CaseRoleDefinition role : roles) {
            String roleName = role.getName();
            Element roleElement = planItemXML.getOwnerDocument().createElement("Role");
            planItemXML.appendChild(roleElement);
            roleElement.setAttribute("name", roleName);
        }
    }
}
