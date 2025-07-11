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

package org.cafienne.engine.processtask.implementation.calculation.definition;

import org.cafienne.engine.cmmn.definition.CMMNElementDefinition;
import org.cafienne.engine.cmmn.definition.ModelDefinition;
import org.cafienne.json.Value;
import org.cafienne.engine.processtask.implementation.calculation.Calculation;
import org.cafienne.engine.processtask.implementation.calculation.CalculationDefinition;
import org.cafienne.engine.processtask.implementation.calculation.Result;
import org.cafienne.engine.processtask.implementation.calculation.definition.expression.CalculationExpressionDefinition;
import org.cafienne.engine.processtask.implementation.calculation.definition.expression.ConditionDefinition;
import org.cafienne.engine.processtask.implementation.calculation.definition.source.InputReference;
import org.cafienne.engine.processtask.implementation.calculation.definition.source.SourceDefinition;
import org.cafienne.engine.processtask.implementation.calculation.operation.CalculationStep;
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
    private final List<InputReference> inputReferences = new ArrayList<>();
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

    protected StepDefinition(CalculationDefinition calculationDefinition, String identifier) {
        super(calculationDefinition.getElement(), calculationDefinition.getModelDefinition(), calculationDefinition);
        this.identifier = identifier;
        this.expression = null;
        this.condition = null;
        this.inputs = new ArrayList<>();
    }

    /**
     * Utility method for stream steps. Assures a single input only and returns that name
     *
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
    public CalculationStep createInstance(Calculation calculation) {
        return new CalculationStep(calculation, this);
    }

    public ConditionDefinition getCondition() {
        return condition;
    }

    public Result getResult(Calculation calculation, CalculationStep step, Map<InputReference, Value<?>> inputs) {
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

    @Override
    public boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
