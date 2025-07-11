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

package org.cafienne.engine.cmmn.definition.parameter;

import org.cafienne.engine.cmmn.definition.CMMNElementDefinition;
import org.cafienne.engine.cmmn.definition.ModelDefinition;
import org.cafienne.engine.cmmn.definition.casefile.CaseFileItemDefinition;
import org.w3c.dom.Element;

/**
 * Implementation of CMMN spec 5.4.10.3
 * Note that CaseParameterDefinition is also used for Parameter (5.4.10.1) and ProcessParameter (not defined in the specification) at this moment.
 */
public class ParameterDefinition extends CMMNElementDefinition {
    private final String bindingRef;
    private CaseFileItemDefinition binding;
    private final BindingRefinementDefinition bindingRefinement;

    public ParameterDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        bindingRef = parseAttribute("bindingRef", false, "");
        bindingRefinement = parse("bindingRefinement", BindingRefinementDefinition.class, false);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        if (!bindingRef.isEmpty()) {
            binding = getCaseDefinition().findCaseFileItem(bindingRef);
            if (binding == null) {
                getModelDefinition().addReferenceError("The parameter " + getName() + " has a binding to '" + bindingRef + "' but the corresponding case file item cannot be found");
            }
        }
    }

    @Override
    public String getContextDescription() {
        return "The parameter " + getName();
    }

    /**
     * Returns the (optional) case file element to which this parameter is bound
     *
     * @return
     */
    public CaseFileItemDefinition getBinding() {
        return binding;
    }

    /**
     * Returns the (optional) expression to be used to further refine the parameter value
     *
     * @return
     */
    public BindingRefinementDefinition getBindingRefinement() {
        return bindingRefinement;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameParameter);
    }

    public boolean sameBinding(ParameterDefinition other) {
        return same(binding, other.binding);
    }

    public boolean sameBindingRefinement(ParameterDefinition other) {
        return same(bindingRefinement, other.bindingRefinement);
    }

    public boolean sameParameter(ParameterDefinition other) {
        return sameIdentifiers(other)
                && sameBinding(other)
                && sameBindingRefinement(other);
    }
}
