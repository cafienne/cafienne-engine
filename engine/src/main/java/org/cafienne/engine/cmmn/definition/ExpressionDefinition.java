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

package org.cafienne.engine.cmmn.definition;

import org.cafienne.engine.cmmn.expression.CMMNExpressionEvaluator;
import org.cafienne.engine.cmmn.expression.DefaultValueEvaluator;
import org.w3c.dom.Element;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Implementation of 5.4.7 Expressions have a language and a body. When an expression is encountered (e.g., inside a repetition rule) then an Evaluator will be instantiated for that expression. This
 * evaluator is based on the language of the expression. The engine will look for a class named ExpressionEvaluator inside a package that is created based on the language, following the
 * convention "org.cafienne.engine.cmmn.expression.[language]". The constructor of the ExpressionEvaluator class must take {@link ExpressionDefinition} as an argument. Examples:
 * <ul>
 * <li>ExpressionEvaluator</li>
 * <li>org.cafienne.engine.cmmn.expression.xpath.ExpresionEvaluator</li>
 * <li>ExpressionEvaluator</li>
 * </ul>
 */
public class ExpressionDefinition extends CMMNElementDefinition {
    private final String language;
    private final String body;
    private final CMMNExpressionEvaluator evaluator;

    public ExpressionDefinition(ModelDefinition definition, CMMNElementDefinition parentElement, boolean defaultValue) {
        super(null, definition, parentElement);
        this.language = "";
        this.body = "";
        this.evaluator = new DefaultValueEvaluator(defaultValue);
    }

    public ExpressionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        language = parseAttribute("language", false, modelDefinition.getDefaultExpressionLanguage());
        body = parseExpressionString("body", true);
        if (body == null || body.isBlank()) {
            getModelDefinition().addDefinitionError(this.getContextDescription() + " has an empty expression");
        }
        this.evaluator = instantiateEvaluator();
    }

    private CMMNExpressionEvaluator instantiateEvaluator() {
        String evaluatorClassName = "org.cafienne.engine.cmmn.expression." + language + ".ExpressionEvaluator";
        try {
            Class<?> evaluatorClass = Class.forName(evaluatorClassName);
            if (!CMMNExpressionEvaluator.class.isAssignableFrom(evaluatorClass)) {
                throw new NoSuchMethodException("The class " + evaluatorClassName + " must implement " + CMMNExpressionEvaluator.class.getName() + ", but it does not");
            }
            Constructor<?> implementationConstructor = evaluatorClass.getConstructor(ExpressionDefinition.class);
            return (CMMNExpressionEvaluator) implementationConstructor.newInstance(this);
        } catch (ClassNotFoundException e) {
            getModelDefinition().fatalError("The expression language '" + language + "' is not supported", e);
        } catch (NoSuchMethodException e) {
            getModelDefinition().fatalError("The class " + evaluatorClassName + " does not have a constructor that takes " + ExpressionDefinition.class.getName() + " as an argument", e);
        } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            getModelDefinition().fatalError("The class " + evaluatorClassName + " cannot be instantiated", e);
        }
        return null;
    }

    @Override
    public String getContextDescription() {
        if (getParentElement() != null) {
            return getParentElement().getContextDescription();
        } else {
            return "The expression with id " + this.getId();
        }
    }

    public String getLanguage() {
        return language;
    }

    public String getBody() {
        return body;
    }

    public CMMNExpressionEvaluator getEvaluator() {
        return evaluator;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameExpression);
    }

    protected boolean sameExpression(ExpressionDefinition other) {
        return same(this.language, other.language)
                && same(this.body, other.body);
    }
}
