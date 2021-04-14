package org.cafienne.processtask.implementation.calculation.definition.expression;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.MapStepDefinition;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

public class MapExpressionDefinition extends CalculationExpressionDefinition {
    private final String inputName;
    private final String elementName;

    public MapExpressionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        MapStepDefinition parent = getParentElement();
        inputName = parent.assertOneInput();
        elementName = parseAttribute("element", false, inputName);
    }

    @Override
    public Result getResult(Calculation calculation, CalculationStep step, ValueMap sourceMap) {
        return new ResultCreator(calculation, step, sourceMap).result;
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

        ResultCreator(Calculation calculation, CalculationStep step, ValueMap sourceMap) {
            this.calculation = calculation;
            this.step = step;
            this.input = sourceMap.get(inputName); // Take the input by it's original name
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
            ValueMap sourceMap = new ValueMap(elementName, item);
            return evaluateExpression(calculation, step, sourceMap);
        }
    }
}
