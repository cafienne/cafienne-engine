/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.parameter;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
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
    protected boolean equalsWith(Object object) {
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
