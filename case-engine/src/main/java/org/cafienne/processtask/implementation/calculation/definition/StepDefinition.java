/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.definition;

import org.cafienne.json.Value;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.expression.CalculationExpressionDefinition;
import org.cafienne.processtask.implementation.calculation.definition.expression.ConditionDefinition;
import org.cafienne.processtask.implementation.calculation.definition.source.InputReference;
import org.cafienne.processtask.implementation.calculation.definition.source.SourceDefinition;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.cafienne.processtask.implementation.calculation.operation.Source;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StepDefinition extends CMMNElementDefinition implements SourceDefinition {
    private final CalculationExpressionDefinition expression;

    private final List<String> inputs;
    private final List<InputReference> inputReferences = new ArrayList();
    private final String identifier;
    private final ConditionDefinition condition;

    public StepDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        this(element, processDefinition, parentElement, CalculationExpressionDefinition.class);
    }

    protected StepDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement, Class<? extends CalculationExpressionDefinition> expresssionType) {
        super(element, processDefinition, parentElement);
        this.inputs = XMLHelper.getChildrenWithTagName(element, "input").stream().map(child -> XMLHelper.getContent(child, null, "")).filter(ref -> !ref.isBlank()).collect(Collectors.toList());
        parse("input", InputReference.class, inputReferences);
        this.identifier = parseAttribute("output", true);
        this.expression = parse("expression", expresssionType, true);
        this.condition = parse("condition", ConditionDefinition.class, false);
    }

    /**
     * Utility method for stream steps. Assures a single input only and returns that name
     * @return
     */
    public InputReference assertOneInput() {
        if (inputReferences.size() != 1) {
            this.getProcessDefinition().addDefinitionError(this.getDescription() + " must have precisely 1 input reference; found " + inputs.size() + " inputs");
        }
        return inputReferences.get(0);
    }

    @Override
    public boolean hasDependency(StepDefinition stepDefinition) {
        return this.inputReferences.stream().filter(step -> step.getSourceReference().equals(stepDefinition.identifier)).count() > 0;
    }

    @Override
    public Source createInstance(Calculation calculation) {
        return new CalculationStep(calculation, this);
    }

    public ConditionDefinition getCondition() {
        return condition;
    }

    public Result getResult(Calculation calculation, CalculationStep step, Map<InputReference, Value> inputs) {
        return expression.getResult(calculation, step, inputs);
    }

    public Collection<InputReference> getInputs() {
        return inputReferences;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getType() {
        return "Step";
    }
}
