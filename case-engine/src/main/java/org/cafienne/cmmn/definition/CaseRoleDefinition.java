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

import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

/**
 * Implementation of CMMN 1.0 5.2.2: Roles.
 * A case role in the engine has 2 possible extensions: mutex and singleton.
 * The mutex element can be filled with references to other roles. The engine will validate that users in the case team will not have roles
 * that exclude each other.
 * The singleton element indicates that only one person in the case team is allowed to have that role. Through this mechanism it is possible to enable
 * multiple tasks to be handled by the same person.
 *
 */
public class CaseRoleDefinition extends CMMNElementDefinition {
    private Collection<CaseRoleDefinition> mutexRoles = new ArrayList();
    private Collection<String> mutexRoleReferences = new ArrayList();
    private final boolean isSingleton;

    public CaseRoleDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        if (getName() == null) {
            definition.addDefinitionError("A role element without a name was encountered. Role is not added to the case definition. XML element:\n" + XMLHelper.printXMLNode(element));
        }

        // Parse mutex roles.
        parseExtension("cafienne:mutex", String.class, mutexRoleReferences);

        // Figure out if we are singleton role.
        isSingleton = Boolean.parseBoolean(parseExtension("cafienne:singleton", String.class));
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();

        for (String string : mutexRoleReferences) {
            String errorMessage = "Case role " + this.getName();
            CaseRoleDefinition mutexRole = getCaseDefinition().resolveRoleReference(string, errorMessage);
            mutexRoles.add(mutexRole);
            mutexRole.mutexRoles.add(this);
        }
    }

    /**
     * Returns the roles that are mutually exclusive to this role, i.e., users can not have this role
     * as well as one of the roles in the collection both assigned to them.
     * @return
     */
    public Collection<CaseRoleDefinition> getMutexRoles() {
        return mutexRoles;
    }

    /**
     * If a role is a singleton role, only one user can be assigned to this role.
     * @return
     */
    public boolean isSingleton() {
        return isSingleton;
    }

    static void createEmptyDefinition(CaseDefinition caseDefinition) {
        Element emptyXMLRole = caseDefinition.getElement().getOwnerDocument().createElement("caseRole");
        caseDefinition.getElement().appendChild(emptyXMLRole);
        emptyXMLRole.setAttribute("id", "all_across_empty_role");
        emptyXMLRole.setAttribute("name", "");
        emptyXMLRole.setAttribute("description", "");
    }
}