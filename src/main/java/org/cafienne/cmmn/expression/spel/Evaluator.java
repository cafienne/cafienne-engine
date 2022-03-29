package org.cafienne.cmmn.expression.spel;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class Evaluator {
    private final ExpressionParser parser;
    private final Expression expression;
    private final String source;
    private final CMMNElementDefinition definition;

    public Evaluator(CMMNElementDefinition definition, String source) {
        this.parser = new SpelExpressionParser();
        this.definition = definition;
        this.source = source;
        this.expression = parseExpression();
    }

    public boolean isValid() {
        return expression != null;
    }

    public Expression getExpression() {
        return expression;
    }

    private Expression parseExpression() {
        if (source.isBlank()) {
            // Empty expressions lead to IllegalStateException("no node") --> checking here gives a somewhat more understandable error ...
            definition.getModelDefinition().addDefinitionError(definition.getContextDescription() + " has no expression");
            return null;
        }

        try {
            return parser.parseExpression(source);
        } catch (Exception spe) { // Parser uses Assert class of Spring, which raises also exceptions like IllegalStateException
            definition.getModelDefinition().addDefinitionError(definition.getContextDescription() + " has an invalid expression:\n" + spe.getMessage());
            return null;
        }
    }

    public <T> T evaluate(APIRootObject<?> rootObject) {
        ModelActor actor = rootObject.getActor();
        // System.out.println("Now evaluating the expression " + definition.getBody());
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);
        // The case file accessor can be used to dynamically resolve properties that belong to the case file
        SpelReadableRecognizer spelPropertyReader = new SpelReadableRecognizer(actor);
        context.addPropertyAccessor(spelPropertyReader);

        // TODO: improve the type checking and raise better error message if we're getting back the wrong type.

        try {
            actor.addDebugInfo(() -> "Evaluating " + rootObject.getDescription() + ": " + source.trim());
            // Not checking it. If it fails, it really fails.
            @SuppressWarnings("unchecked")
            T value = (T) expression.getValue(context);
            String valueType = value == null ? "NULL" : value.getClass().getSimpleName();
            actor.addDebugInfo(() -> "Outcome has type " + valueType + " and value: " + value);
            return value;
        } catch (EvaluationException invalidExpression) {
            actor.addDebugInfo(() -> "Failure in evaluating " + rootObject.getDescription() + ", with expression " + source.trim(), invalidExpression);
            throw new InvalidExpressionException("Could not evaluate " + expression.getExpressionString() + "\n" + invalidExpression.getLocalizedMessage(), invalidExpression);
        }
    }

}
