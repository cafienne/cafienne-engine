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

package com.casefabric.cmmn.definition.sentry;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.definition.casefile.CaseFileItemDefinition;
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition;
import com.casefabric.cmmn.instance.sentry.CaseFileItemOnPart;
import com.casefabric.cmmn.instance.sentry.Criterion;
import org.w3c.dom.Element;

public class CaseFileItemOnPartDefinition extends OnPartDefinition {
    private final CaseFileItemTransition standardEvent;
    private final String sourceRef;
    private CaseFileItemDefinition source;

    public CaseFileItemOnPartDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        String standardEventName = parseString("standardEvent", true);
        standardEvent = CaseFileItemTransition.getEnum(standardEventName);
        if (standardEvent == null) {
            getCaseDefinition().addDefinitionError("A standard event named " + standardEventName + " does not exist for case file items");
        }
        sourceRef = parseAttribute("sourceRef", true);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        // Add a check that the source only has to be filled if the standardEvent is specified
        source = getCaseDefinition().findCaseFileItem(sourceRef);
        if (source == null) {
            getModelDefinition().addReferenceError("A case file item with name '" + sourceRef + "' is referenced from sentry " + getParentElement().getName() + ", but it does not exist in the case file model");
        }
    }

    @Override
    public String getContextDescription() {
        return source.getType() + "[" + source.getPath() + "]." + standardEvent;
    }

    public CaseFileItemTransition getStandardEvent() {
        return standardEvent;
    }

    @Override
    public CaseFileItemDefinition getSourceDefinition() {
        return source;
    }

    @Override
    public CaseFileItemOnPartDefinition asFile() {
        return this;
    }

    @Override
    public CaseFileItemOnPart createInstance(Criterion<?> criterion) {
        return new CaseFileItemOnPart(criterion, this);
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameOnPart);
    }

    public boolean sameOnPart(CaseFileItemOnPartDefinition other) {
        return same(standardEvent, other.standardEvent)
                && same(source, other.source);
    }
}