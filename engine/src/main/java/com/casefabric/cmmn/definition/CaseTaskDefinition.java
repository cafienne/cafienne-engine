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

package com.casefabric.cmmn.definition;

import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.PlanItemType;
import com.casefabric.cmmn.instance.Stage;
import com.casefabric.cmmn.instance.task.cmmn.CaseTask;
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
        }
    }

    @Override
    protected void validateElement() {
        super.validateElement();
        if (getImplementationDefinition().equals(this.getCaseDefinition())) {
            PlanItemStarter starter = findItemDefinition().getStarter();
            if (starter.isImmediate()) {
                // Consideration: analyze the dependency chain and if there is a timer starter, it is ok if we have exits.
                //  Note: this code is available, we can ask the PlanItemStarter for it. However, current consideration
                //  would be that if someone needs this functionality, they can contact us and explain the use case :)
                getCaseDefinition().addDefinitionError("CaseTask '"+getName()+"' leads to infinite recursion, because\n " + starter);
            }
        }
    }

    @Override
    public PlanItemType getItemType() {
        return PlanItemType.CaseTask;
    }

    @Override
    public CaseTask createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        return new CaseTask(id, index, itemDefinition, this, stage);
    }

    @Override
    public CaseDefinition getImplementationDefinition() {
        return subCaseDefinition;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameCaseTask);
    }

    public boolean sameCaseTask(CaseTaskDefinition other) {
        return sameTask(other)
                && subCaseDefinition.sameIdentifiers(other.subCaseDefinition);
    }
}
