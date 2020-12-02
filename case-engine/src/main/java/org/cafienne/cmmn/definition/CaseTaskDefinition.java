/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.task.cmmn.CaseTask;
import org.w3c.dom.Element;

public class CaseTaskDefinition extends TaskDefinition<CaseDefinition> {
    private final String caseRef;
    private CaseDefinition subCaseDefinition;

    public CaseTaskDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.caseRef = parseAttribute("caseRef", true);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();

        this.subCaseDefinition = getCaseDefinition().getDefinitionsDocument().getCaseDefinition(this.caseRef);
        if (this.subCaseDefinition == null) {
            getCaseDefinition().addReferenceError("The case task '" + this.getName() + "' refers to a case named " + caseRef + ", but that definition is not found");
            return; // Avoid further checking on this element
        }
    }

    @Override
    public CaseTask createInstance(String id, int index, ItemDefinition itemDefinition, Stage stage, Case caseInstance) {
        return new CaseTask(id, index, itemDefinition, this, stage);
    }

	@Override
	public CaseDefinition getImplementationDefinition() {
		return subCaseDefinition;
	}
}
