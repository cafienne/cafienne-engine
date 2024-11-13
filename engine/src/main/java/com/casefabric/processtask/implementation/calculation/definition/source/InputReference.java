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

package com.casefabric.processtask.implementation.calculation.definition.source;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.processtask.implementation.calculation.CalculationDefinition;
import com.casefabric.processtask.implementation.calculation.definition.StepDefinition;
import org.w3c.dom.Element;

public class InputReference extends CMMNElementDefinition {
    private final String sourceReference;
    private final String elementName;
    private SourceDefinition source;

    public InputReference(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.sourceReference = parseAttribute("source", true);
        this.elementName = parseAttribute("as", false, sourceReference);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        StepDefinition step = getParentElement();
        CalculationDefinition calculationDefinition = step.getParentElement();
        source = calculationDefinition.getSource(sourceReference);
        // Make sure source is defined
        if (source == null) {
            this.getProcessDefinition().addDefinitionError("Cannot find input '" + sourceReference + "' in step['" + step.getIdentifier() + "']");
        }
        // Make sure source is not dependent on us too
        if (source.hasDependency(step)) {
            this.getProcessDefinition().addDefinitionError(step.getDescription() + " has a recursive reference to " + source.getDescription());
        }
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public String getElementName() {
        return elementName;
    }

    public SourceDefinition getSource() {
        return source;
    }

    @Override
    public boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
