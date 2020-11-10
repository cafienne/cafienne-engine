/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.casefile;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.w3c.dom.Element;

public class CaseFileDefinition extends CaseFileItemCollectionDefinition {
    public CaseFileDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        if (element != null) {
            parse("caseFileItem", CaseFileItemDefinition.class, getChildren());
            if (getChildren().size() < 1) {
                definition.addDefinitionError("The case file must have at least one case file item");
            }
        }
    }
}
