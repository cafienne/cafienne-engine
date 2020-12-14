/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.cafienne.cmmn.expression.CMMNExpressionEvaluator;
import org.cafienne.cmmn.expression.DefaultValueEvaluator;
import org.w3c.dom.Element;

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
    private String language;
    private String body;
    private CMMNExpressionEvaluator evaluator;

    public ExpressionDefinition(ModelDefinition definition, CMMNElementDefinition parentElement, boolean defaultValue) {
        super(null, definition, parentElement);
        this.evaluator = new DefaultValueEvaluator(defaultValue);
    }

    public ExpressionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        language = element.getAttribute("language");
        body = parse("body", String.class, true);
        if (body == null || body.isBlank()) {
            getModelDefinition().addDefinitionError(this.getContextDescription() + " has an empty expression");
        }
        String evaluatorClassName = "org.cafienne.cmmn.expression." + language + ".ExpressionEvaluator";
        try {
            Class<?> evaluatorClass = Class.forName(evaluatorClassName);
            if (!CMMNExpressionEvaluator.class.isAssignableFrom(evaluatorClass)) {
                throw new NoSuchMethodException("The class " + evaluatorClassName + " must implement " + CMMNExpressionEvaluator.class.getName() + ", but it does not");
            }
            Constructor<?> implementationConstructor = evaluatorClass.getConstructor(ExpressionDefinition.class);
            this.evaluator = (CMMNExpressionEvaluator) implementationConstructor.newInstance(this);
        }
        catch (ClassNotFoundException e) {
            getModelDefinition().fatalError("The expression language '" + language +"' is not supported", e);
        } catch (NoSuchMethodException e) {
            getModelDefinition().fatalError("The class " + evaluatorClassName + " does not have a constructor that takes " + ExpressionDefinition.class.getName() + " as an argument", e);
        } catch (SecurityException e) {
            getModelDefinition().fatalError("The class " + evaluatorClassName + " cannot be instantiated", e);
        } catch (InstantiationException e) {
            getModelDefinition().fatalError("The class " + evaluatorClassName + " cannot be instantiated", e);
        } catch (IllegalAccessException e) {
            getModelDefinition().fatalError("The class " + evaluatorClassName + " cannot be instantiated", e);
        } catch (IllegalArgumentException e) {
            getModelDefinition().fatalError("The class " + evaluatorClassName + " cannot be instantiated", e);
        } catch (InvocationTargetException e) {
            getModelDefinition().fatalError("The class " + evaluatorClassName + " cannot be instantiated", e);
        }
    }

    @Override
    public String getContextDescription() {
        String description = "";
        if (getParentElement()!=null) {
            description = getParentElement().getContextDescription();
        }
        if (description.trim().isEmpty()) {
            description = "The expression with id "+this.getId();
        }
        return description;
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
}
