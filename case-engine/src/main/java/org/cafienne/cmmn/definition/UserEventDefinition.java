/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.*;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class UserEventDefinition extends PlanItemDefinitionDefinition {
    private final String authorizedRoleRefs;
    private final Collection<CaseRoleDefinition> authorizedRoles = new ArrayList();

    public UserEventDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        authorizedRoleRefs = parseAttribute("authorizedRoleRefs", false, "");
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

    public UserEvent createInstance(String id, int index, ItemDefinition itemDefinition, Stage stage, Case caseInstance) {
        return new UserEvent(id, index, itemDefinition, this, stage);
    }

    @Override
    public Transition getEntryTransition() {
        return Transition.Occur;
    }
}
