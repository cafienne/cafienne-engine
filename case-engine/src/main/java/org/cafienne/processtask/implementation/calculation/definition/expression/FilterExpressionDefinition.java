package org.cafienne.processtask.implementation.calculation.definition.expression;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.FilterStepDefinition;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

public class FilterExpressionDefinition extends ConditionDefinition {
    public FilterExpressionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }

    @Override
    public Result getResult(Calculation calculation, CalculationStep step, ValueMap sourceMap) {
        FilterStepDefinition parent = getParentElement();
        String inputName = parent.getSource().getIdentifier();
        Value input = sourceMap.get(inputName);
        Value filteredInput = getFilteredValue(calculation, step, inputName, input);
        return new Result(calculation, step, filteredInput);
    }

    private Value getFilteredValue(Calculation calculation, CalculationStep step, String inputName, Value input) {
        if (input.isList()) {
            // Filter the list and return a list with the filtered items only.
            Object[] items = input.asList().stream().filter(item -> isFilteredItem(calculation, step, inputName, item)).toArray();
            // Note: items is always an array of type Value
            return new ValueList(items);
        } else {
            // Instead of the list, we will only check if the given input object matches the filter.
            // If so, then we return that object, otherwise we return a null value (is that the best choice?)
            if (isFilteredItem(calculation, step, inputName, input)) {
                return input;
            } else {
                return Value.NULL;
            }
        }
    }

    private boolean isFilteredItem(Calculation calculation, CalculationStep step, String inputName, Value item) {
        // run the expression and add the result to the target list
        ValueMap itemInput = new ValueMap(inputName, item);
        return super.getBooleanResult(calculation, step, itemInput);
    }

    @Override
    public String getType() {
        return "Filter";
    }
}
