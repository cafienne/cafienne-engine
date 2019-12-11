/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import java.util.ArrayList;
import java.util.Collection;

import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.UserEvent;
import org.w3c.dom.Element;

public class UserEventDefinition extends PlanItemDefinitionDefinition {
    private final String authorizedRoleRefs;
    private final Collection<CaseRoleDefinition> authorizedRoles = new ArrayList<CaseRoleDefinition>();

    public UserEventDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        authorizedRoleRefs = parseAttribute("authorizedRoleRefs", false, "");
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        getCaseDefinition().resolveRoleReferences(authorizedRoleRefs, authorizedRoles, "User Event " + this);
    }

    /**
     * Returns the collection of case roles that are allowed to raise this user event
     *
     * @return
     */
    public Collection<CaseRoleDefinition> getAuthorizedRoles() {
        return authorizedRoles;
    }

    public UserEvent createInstance(PlanItem planItem) {
        return new UserEvent(planItem, this);
    }

    @Override
    public Transition getEntryTransition() {
        return Transition.Occur;
    }
}
