package org.cafienne.processtask.implementation.calculation.definition.expression;

import org.cafienne.json.Value;
import org.cafienne.json.ValueList;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.MapStepDefinition;
import org.cafienne.processtask.implementation.calculation.definition.source.InputReference;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

public class MapExpressionDefinition extends CalculationExpressionDefinition {
    private final InputReference inputReference;

    public MapExpressionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        MapStepDefinition parent = getParentElement();
        inputReference = parent.assertOneInput();
    }

    @Override
    public Result getResult(Calculation calculation, CalculationStep step, Map<InputReference, Value> sourceMap) {
        return new ResultCreator(calculation, step, sourceMap.get(inputReference)).result;
    }

    @Override
    public String getType() {
        return "Mapping step";
    }

    class ResultCreator {
        private final Calculation calculation;
        private final CalculationStep step;
        private final Value input;
        private final Result result;

        ResultCreator(Calculation calculation, CalculationStep step, Value input) {
            this.calculation = calculation;
            this.step = step;
            this.input = input;
            this.result = new Result(calculation, step, getMappedValue());
        }

        private Value getMappedValue() {
            if (input.isList()) {
                // Map the list and return a list with the mapped items.
                Object[] items = input.asList().stream().map(this::mapItem).toArray();
                // Note: items is always an array of type Value
                return new ValueList(items);
            } else {
                // Instead of the list, we will only map the given input object.
                return mapItem(input);
            }
        }

        private Value mapItem(Value item) {
            // In the expression, the input element can only be accessed through the element name
            Map<InputReference, Value> mappableInput = new HashMap();
            mappableInput.put(inputReference, item);
            return evaluateExpression(calculation, step, mappableInput);
        }
    }
}
