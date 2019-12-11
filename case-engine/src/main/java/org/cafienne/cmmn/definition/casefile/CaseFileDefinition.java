/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.casefile;

import java.util.Collection;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.w3c.dom.Element;

public class CaseFileDefinition extends CaseFileItemCollectionDefinition {
    public CaseFileDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        if (element != null) {
            parse("caseFileItem", CaseFileItemDefinition.class, getCaseFileItems());
            if (getCaseFileItems().size() < 1) {
                definition.addDefinitionError("The case file must have at least one case file item");
            }
        }
    }

    public Collection<CaseFileItemDefinition> getCaseFileItems() {
        return getItems();
    }

    public CaseFileItemDefinition findCaseFileItem(String identifier) {
        CaseFileItemDefinition item = getCaseFileItems().stream().filter(i -> {
            String name = i.getName();
            String id = i.getId();
            return name.equals(identifier) || id.equals(identifier);
        }).findFirst().orElse(null);
        if (item == null) {
            for (CaseFileItemDefinition caseFileItem : getCaseFileItems()) {
                item = caseFileItem.findCaseFileItem(identifier);
                if (item != null) {
                    return item;
                }
            }
        }
        return item;
    }
}
