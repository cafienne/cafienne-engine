package org.cafienne.processtask.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.expression.spel.Evaluator;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.springframework.expression.Expression;

public class ParameterEvaluator extends Evaluator {
    private final String parameterName;

    public ParameterEvaluator(CMMNElementDefinition definition, String source) {
        super(definition, source);
        this.parameterName = source;
    }

    @Override
    protected Expression parseExpression() {
        return null;
    }

    /**
     * Always returns a string representation of the parameter value, or null.
     */
    @Override
    public <T> T evaluate(APIRootObject<?> rootObject) {
        @SuppressWarnings("unchecked")
        T i_m_always_a_string = (T) returnValue(rootObject, () -> {
            Object value = rootObject.read(parameterName);
            if (value == null) return null;
            return String.valueOf(value);
        });
        return i_m_always_a_string;
    }
}
