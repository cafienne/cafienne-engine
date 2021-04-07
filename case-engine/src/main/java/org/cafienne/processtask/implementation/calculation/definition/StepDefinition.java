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
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.CalculationDefinition;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.cafienne.processtask.implementation.calculation.operation.Source;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StepDefinition extends CMMNElementDefinition implements SourceDefinition {
    private final CalculationDefinition parent;
    private final CalculationExpressionDefinition expression;

    private final List<String> inputReferences;
    private final String identifier;
    private final Map<String, SourceDefinition> sources = new HashMap();
    private final ConditionDefinition condition;

    public StepDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.inputReferences = XMLHelper.getChildrenWithTagName(element, "input").stream().map(child -> XMLHelper.getContent(child, null, "")).filter(ref -> !ref.isBlank()).collect(Collectors.toList());
        this.identifier = parseAttribute("output", true);
        this.parent = (CalculationDefinition) parentElement; // Should not fail, otherwise structure has changed...
        this.expression = parse("expression", CalculationExpressionDefinition.class, true);
        this.condition = parse("condition", ConditionDefinition.class, false);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        // Parse the incoming sources and validate that there are no recursive dependencies with other steps
        this.inputReferences.forEach(sourceReference -> {
            SourceDefinition source = parent.getSource(sourceReference);
            // Make sure source is defined
            if (source == null) {
                this.getProcessDefinition().addDefinitionError("Cannot find input '" + sourceReference + "' in step['" + identifier + "']");
            }
            // Make sure source is not dependent on us too
            if (source.hasDependency(this)) {
                this.getProcessDefinition().addDefinitionError(this.getDescription() + " has a recursive reference to " + source.getDescription());
            }
            // That's a valid source then, add it to our incoming dependencies
            sources.put(sourceReference, source);
        });
    }

    @Override
    public boolean hasDependency(StepDefinition stepDefinition) {
        return this.sources.containsKey(stepDefinition.identifier);
    }

    @Override
    public Source createInstance(Calculation calculation) {
        return new CalculationStep(calculation, this);
    }

    public ConditionDefinition getCondition() {
        return condition;
    }

    public CalculationExpressionDefinition getExpression() {
        return expression;
    }

    public Collection<SourceDefinition> getSources() {
        return sources.values();
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getType() {
        return "Step";
    }
}
