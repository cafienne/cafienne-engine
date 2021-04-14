/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.definition.expression.FilterExpressionDefinition;
import org.cafienne.processtask.implementation.calculation.definition.source.SourceDefinition;
import org.w3c.dom.Element;

public class FilterStepDefinition extends StepDefinition {
    private SourceDefinition source;

    public FilterStepDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement, FilterExpressionDefinition.class);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        if (getSources().size() != 1) {
            this.getProcessDefinition().addDefinitionError(this.getDescription() + " must have precisely 1 input reference; found " + getSources().size() + " inputs");
        }
        this.source = getSources().stream().findFirst().orElse(null);
    }

    public SourceDefinition getSource() {
        return source;
    }

    @Override
    public String getType() {
        return "Filter";
    }
}
