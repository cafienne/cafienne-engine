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

package org.cafienne.engine.cmmn.definition.team;

import org.cafienne.engine.cmmn.definition.CMMNElementDefinition;
import org.cafienne.engine.cmmn.definition.ModelDefinition;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

/**
 * Implementation of CMMN 1.0 5.2.2: Roles.
 * A case role in the engine has 2 possible extensions: mutex and singleton.
 * The mutex element can be filled with references to other roles. The engine will validate that users in the case team will not have roles
 * that exclude each other.
 * The singleton element indicates that only one person in the case team is allowed to have that role. Through this mechanism it is possible to enable
 * multiple tasks to be handled by the same person.
 */
public class CaseRoleDefinition extends CMMNElementDefinition {
    private static final String emptyRoleIdentifier = "all_across_empty_role";
    private final boolean isEmptyRole;

    public CaseRoleDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        this(element, modelDefinition, parentElement, false);
    }

    private boolean isDefaultEmptyRole() {
        // The empty role is constructed "by code" in new cases and not serialized into the original source of the case definition.
        //  However, in the past, the role did end up in serialized case definitions, and in order to remain compatible we also scan
        //  explicitly on (id == "all_across_empty_role").
        return isEmptyRole || getId().equals(emptyRoleIdentifier);
    }

    private CaseRoleDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement, boolean isEmptyRole) {
        super(element, modelDefinition, parentElement);
        this.isEmptyRole = isEmptyRole;
        if (getName() == null || getName().isBlank()) {
            if (! isDefaultEmptyRole()) {
                modelDefinition.addDefinitionError("A role element without a name was encountered. Role is not added to the case definition. XML element:\n" + XMLHelper.printXMLNode(element));
            }
        }
    }

    static CaseRoleDefinition createEmptyDefinition(CMMNElementDefinition definitionElement) {
        Element emptyXMLRole = definitionElement.getElement().getOwnerDocument().createElement("caseRole");
        emptyXMLRole.setAttribute("id", emptyRoleIdentifier);
        emptyXMLRole.setAttribute("name", "");
        emptyXMLRole.setAttribute("description", "");
        return new CaseRoleDefinition(emptyXMLRole, definitionElement.getModelDefinition(), definitionElement, true);
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameRole);
    }

    public boolean sameRole(CaseRoleDefinition other) {
        return sameIdentifiers(other);
    }
}