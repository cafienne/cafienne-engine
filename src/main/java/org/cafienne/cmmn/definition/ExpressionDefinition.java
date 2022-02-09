/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.expression.CMMNExpressionEvaluator;
import org.cafienne.cmmn.expression.DefaultValueEvaluator;
import org.w3c.dom.Element;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Implementation of 5.4.7 Expressions have a language and a body. When an expression is encountered (e.g., inside a repetition rule) then an Evaluator will be instantiated for that expression. This
 * evaluator is based on the language of the expression. The engine will look for a class named ExpressionEvaluator inside a package that is created based on the language, following the
 * convention "org.cafienne.cmmn.expression.[language]". The constructor of the ExpressionEvaluator class must take {@link ExpressionDefinition} as an argument. Examples:
 * <ul>
 * <li>ExpressionEvaluator</li>
 * <li>org.cafienne.cmmn.expression.xpath.ExpresionEvaluator</li>
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
        body = parseString("body", true);
        if (body == null || body.isBlank()) {
            getModelDefinition().addDefinitionError(this.getContextDescription() + " has an empty expression");
        }
        this.evaluator = instantiateEvaluator();
    }

    private CMMNExpressionEvaluator instantiateEvaluator() {
        String evaluatorClassName = "org.cafienne.cmmn.expression." + language + ".ExpressionEvaluator";
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
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameExpression);
    }

    protected boolean sameExpression(ExpressionDefinition other) {
        return same(this.language, other.language)
                && same(this.body, other.body);
    }
}
