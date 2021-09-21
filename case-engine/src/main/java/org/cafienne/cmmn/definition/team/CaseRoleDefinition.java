/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.team;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
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
    private final CaseTeamDefinition teamDefinition;

    public CaseRoleDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.teamDefinition = getParentElement();
        if (getName() == null) {
            modelDefinition.addDefinitionError("A role element without a name was encountered. Role is not added to the case definition. XML element:\n" + XMLHelper.printXMLNode(element));
        }
    }

    static CaseRoleDefinition createEmptyDefinition(CMMNElementDefinition definitionElement) {
        Element emptyXMLRole = definitionElement.getElement().getOwnerDocument().createElement("caseRole");
        emptyXMLRole.setAttribute("id", "all_across_empty_role");
        emptyXMLRole.setAttribute("name", "");
        emptyXMLRole.setAttribute("description", "");
        return new CaseRoleDefinition(emptyXMLRole, definitionElement.getModelDefinition(), definitionElement);
    }
}