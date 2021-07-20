/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.team;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * Parsed structure of a case team, i.e. the roles defined in a case definition
 */
public class CaseTeamDefinition extends CMMNElementDefinition {
    private final Collection<CaseRoleDefinition> caseRoles = new ArrayList<>();

    public CaseTeamDefinition(CaseDefinition definition) {
        super(definition.getElement(), definition, definition);

        // The XSD states that the name of the element is caseRoles (rather than caseRole). We also support the tag <caseRole> and here read
        // additionally the standard tag ...
        parse("caseRole", CaseRoleDefinition.class, caseRoles); // Custom because we don't like the 1.0 XSD

        // Some tricky XML parsing to figure out if we are CMMN 1.0 or CMMN 1.1;
        Collection<Element> roleElements = XMLHelper.getChildrenWithTagName(getElement(), "caseRoles");
        if (roleElements.size() != 1) { // It is the 1.0 format
            parse("caseRoles", CaseRoleDefinition.class, caseRoles); // CMMN 1.0
        } else {
            Element roleElement = roleElements.iterator().next();
            if (roleElement.getAttribute("name").isEmpty()) { // No name, we assume it is CMMN1.1
                parseGrandChildren("caseRoles", "role", CaseRoleDefinition.class, caseRoles); // CMMN 1.1
            } else {
                parse("caseRoles", CaseRoleDefinition.class, caseRoles); // CMMN 1.0
            }
        }
        // We add an "Empty Role", such that even if a team member has "no role", they can still become member of the team.
        caseRoles.add(CaseRoleDefinition.createEmptyDefinition(this));
    }

    /**
     * Returns the collection of roles in the case definition, see {@link CaseRoleDefinition}.
     *
     * @return
     */
    public Collection<CaseRoleDefinition> getCaseRoles() {
        return caseRoles;
    }

    /**
     * Returns the role definition with the specified name or id, if it exists, or null if it does not exist.
     *
     * @param roleName
     * @return
     */
    public CaseRoleDefinition getCaseRole(String roleName) {
        return caseRoles.stream().filter(r -> r.getName().equals(roleName) || r.getId().equals(roleName)).findFirst().orElse(null);
    }

    /**
     * Resolves a String of space tokenized role references and adds them to the roles collection
     *
     * @param roleReferences
     * @param roles
     * @param referrer
     */
    public void resolveRoleReferences(String roleReferences, Collection<CaseRoleDefinition> roles, String referrer) {
        StringTokenizer st = new StringTokenizer(roleReferences, " ");
        while (st.hasMoreTokens()) {
            String roleRef = st.nextToken();
            CaseRoleDefinition role = resolveRoleReference(roleRef, referrer);
            if (role != null) {
                roles.add(role);
            }
        }
    }

    /**
     * Resolves a single role reference
     *
     * @param roleRef
     * @param referrer
     * @return
     */
    public CaseRoleDefinition resolveRoleReference(String roleRef, String referrer) {
        CaseRoleDefinition role = getCaseRole(roleRef);
        if (role == null) {
            getModelDefinition().addDefinitionError("A role '" + roleRef + "' is referenced from " + referrer + ", but it cannot be found in the case definition");
        } else {
            return role;
        }
        return null;
    }
}
