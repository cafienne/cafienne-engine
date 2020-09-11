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
import java.util.StringTokenizer;

import org.cafienne.cmmn.definition.casefile.CaseFileDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.definition.task.TaskImplementationContract;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

/**
 * Parsed structure of a case definition (a top-level &lt;case&gt; element within a &lt;definitions&gt; document).
 */
public class CaseDefinition extends Definition implements TaskImplementationContract {
    private final Collection<CaseRoleDefinition> caseRoles = new ArrayList();
    private CaseFileDefinition caseFileModel;
    private CasePlanDefinition casePlanModel;

    public CaseDefinition(Element definitionElement, DefinitionsDocument document) {
        super(definitionElement, document);

        caseFileModel = parse("caseFileModel", CaseFileDefinition.class, true);
        casePlanModel = parse("casePlanModel", CasePlanDefinition.class, true);

        // The XSD states that the name of the element is caseRoles (rather than caseRole). We also support the tag <caseRole> and here read
        // additionally the standard tag ...
        parse("caseRole", CaseRoleDefinition.class, caseRoles); // Custom because we don't like the 1.0 XSD

        // Some tricky XML parsing to figure out if we are CMMN 1.0 or CMMN 1.1;
        Collection<Element> roleElements = XMLHelper.getChildrenWithTagName(getElement(), "caseRoles");
        if (roleElements.isEmpty() || roleElements.size() > 1) { // It is the 1.0 format
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
     * Returns the case plan of this definition.
     *
     * @return
     */
    public CasePlanDefinition getCasePlanModel() {
        return casePlanModel;
    }

    /**
     * Returns the case file model in this definition. The model is an abstraction on top of an actual object model.
     *
     * @return
     */
    public CaseFileDefinition getCaseFileModel() {
        return caseFileModel;
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
    void resolveRoleReferences(String roleReferences, Collection<CaseRoleDefinition> roles, String referrer) {
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
    CaseRoleDefinition resolveRoleReference(String roleRef, String referrer) {
        CaseRoleDefinition role = getCaseRole(roleRef);
        if (role == null) {
            addDefinitionError("A role '" + roleRef + "' is referenced from " + referrer + ", but it cannot be found in the case definition");
        } else {
            return role;
        }
        return null;
    }

    /**
     * Searches the whole case plan for a PlanItem with the specified identifier.
     *
     * @param sourceLocation
     * @param planItemId
     * @return
     */
    public PlanItemDefinition findPlanItem(CMMNElementDefinition sourceLocation, String planItemId) {
        if (sourceLocation == null) {
            return null;
        }
        if (sourceLocation instanceof PlanFragmentDefinition) {
            PlanItemDefinition planItem = ((PlanFragmentDefinition) sourceLocation).searchPlanItem(planItemId);
            if (planItem != null) {
                return planItem;
            }
        }
        return findPlanItem(sourceLocation.getParentElement(), planItemId);
    }

    PlanItemDefinitionDefinition findPlanItemDefinition(String identifier) {
        Collection<PlanItemDefinitionDefinition> planItemDefinitions = getCasePlanModel().getPlanItemDefinitions();
        return planItemDefinitions.stream().filter(p -> {
            return p.getId().equals(identifier) || p.getName().equals(identifier);
        }).findFirst().orElse(null);
    }

    /**
     * Searches within the case file for a CaseFileItem with the specified name
     *
     * @param name
     * @return
     */
    public CaseFileItemDefinition findCaseFileItem(String name) {
        return getCaseFileModel().findCaseFileItem(name);
    }
}
