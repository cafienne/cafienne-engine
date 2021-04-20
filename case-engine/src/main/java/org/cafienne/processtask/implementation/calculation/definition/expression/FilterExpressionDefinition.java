package org.cafienne.processtask.implementation.calculation.definition.expression;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.FilterStepDefinition;
import org.cafienne.processtask.implementation.calculation.definition.source.InputReference;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

public class FilterExpressionDefinition extends ConditionDefinition {
    private final InputReference inputReference;

    public FilterExpressionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        FilterStepDefinition parent = getParentElement();
        inputReference = parent.assertOneInput();
    }

    @Override
    public Result getResult(Calculation calculation, CalculationStep step, Map<InputReference, Value> inputs) {
        return new ResultCreator(calculation, step, inputs.get(inputReference)).result;
    }

    @Override
    public String getType() {
        return "Filter";
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
            this.result = new Result(calculation, step, getFilteredValue());
        }

        private Value getFilteredValue() {
            if (input.isList()) {
                // Filter the list and return a list with the filtered items only.
                Object[] items = input.asList().stream().filter(this::isFilteredItem).toArray();
                // Note: "items" is always an array of type Value
                return new ValueList(items);
            } else {
                // Instead of the list, we will only check if the given input object matches the filter.
                // If so, then we return that object, otherwise we return a null value (is that the best choice?)
                if (isFilteredItem(input)) {
                    return input;
                } else {
                    return Value.NULL;
                }
            }
        }

        private boolean isFilteredItem(Value item) {
            // In the expression, the input element can only be accessed through the element name
            Map<InputReference, Value> filteredInputs = new HashMap();
            filteredInputs.put(inputReference, item);
            return getBooleanResult(calculation, step, filteredInputs);
        }
    }
}
