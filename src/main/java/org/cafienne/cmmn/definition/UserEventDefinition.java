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

import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.*;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class UserEventDefinition extends EventListenerDefinition {
    private final String authorizedRoleRefs;
    private final Collection<CaseRoleDefinition> authorizedRoles = new ArrayList<>();

    public UserEventDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        authorizedRoleRefs = parseAttribute("authorizedRoleRefs", false, "");
    }

    @Override
    public PlanItemType getItemType() {
        return PlanItemType.UserEvent;
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        getCaseDefinition().getCaseTeamModel().resolveRoleReferences(authorizedRoleRefs, authorizedRoles, "User Event " + this);
    }

    /**
     * Returns the collection of case roles that are allowed to raise this user event
     *
     * @return
     */
    public Collection<CaseRoleDefinition> getAuthorizedRoles() {
        return authorizedRoles;
    }

    public UserEvent createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        return new UserEvent(id, index, itemDefinition, this, stage);
    }

    @Override
    public Transition getEntryTransition() {
        return Transition.Occur;
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameUserEvent);
    }

    public boolean sameUserEvent(UserEventDefinition other) {
        return samePlanItemDefinitionDefinition(other)
                && same(authorizedRoles, other.authorizedRoles);
    }
}
