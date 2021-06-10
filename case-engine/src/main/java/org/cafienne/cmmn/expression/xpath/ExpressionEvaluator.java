/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.xpath;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.cafienne.actormodel.command.exception.CommandException;
import org.cafienne.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.expression.CMMNExpressionEvaluator;
import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.cmmn.instance.*;
import org.cafienne.actormodel.serialization.json.BooleanValue;
import org.cafienne.actormodel.serialization.json.Value;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.util.XMLHelper;
import org.cafienne.cmmn.definition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class ExpressionEvaluator implements CMMNExpressionEvaluator {
    private final static Logger logger = LoggerFactory.getLogger(ExpressionEvaluator.class);

    private final String xpathExpression;
    private final ExpressionDefinition definition;
    private final Map<String, String> prefixes = new HashMap();

    public ExpressionEvaluator(ExpressionDefinition expressionDefinition) {
        xpathExpression = expressionDefinition.getBody();
        definition = expressionDefinition;
    }

    private boolean evaluateConstraint(Object contextObject, String ruleTypeDescription) {
        logger.debug("Now evaluating the expression " + definition.getBody());
        // TODO: figure out a place for the namespaces and how to resolve them.
        Collection<Element> namespaceElements = XMLHelper.getChildrenWithTagName(definition.getElement(), "namespace");
        for (Element namespaceElement : namespaceElements) {
            String prefix = namespaceElement.getAttribute("prefix");
            String uri = namespaceElement.getAttribute("uri");
            prefixes.put(prefix, uri);
        }

        Element documentElement = definition.getElement();
        if (logger.isDebugEnabled()) {
            logElementNamespaces(documentElement);
        }

        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            xpath.setNamespaceContext(new NamespaceContext() {

                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    logger.debug("Gettting prefixes for uri " + namespaceURI);

                    return prefixes.values().iterator();
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    logger.debug("Gettting prefix for uri " + namespaceURI);

                    return "";
                }

                @Override
                public String getNamespaceURI(String prefix) {
                    logger.debug("Gettting uri for prefix " + prefix + ", it is: " + prefixes.get(prefix));

                    return prefixes.get(prefix);
                }
            });
            javax.xml.xpath.XPathExpression expr = xpath.compile(xpathExpression);
            String output = String.valueOf(contextObject);
            InputStream inputStream = new ByteArrayInputStream(output.getBytes());
            InputSource is = new InputSource(inputStream);

            boolean value = Boolean.valueOf(expr.evaluate(is));

            // System.out.println("X: "+value);
            return value;
        } catch (Exception allKindsOfException) {
            throw new CommandException("Cannot evaluate XPath expression", allKindsOfException);
        }
    }

    private void logElementNamespaces(Element documentElement) {
        NamedNodeMap attributes = documentElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node a = attributes.item(i);
            String attributeName = a.getNodeName();
            if (attributeName.startsWith("xmlns:")) {
                // It's a definition of a namespace with it's prefix
                String prefix = attributeName.substring(6);
                String namespace = a.getNodeValue();
                logger.debug("NS: " + prefix + "='" + namespace + "'");
            }
        }
    }

    @Override
    public Duration evaluateTimerExpression(TimerEvent timerEvent, TimerEventDefinition definition) {
        // No further context usage right now, just plain string evaluation.
        try {
            return Duration.parse(definition.getTimerExpression().getBody().trim());
        } catch (DateTimeParseException dtpe) {
            throw new InvalidExpressionException("The timer expression " + definition.getTimerExpression().getBody() + " in " + definition.getName() + " cannot be parsed into a Duration", dtpe);
        }
    }

    @Override
    public Value<?> evaluateInputParameterTransformation(Case caseInstance, TaskInputParameter from, ParameterDefinition to, Task<?> task) {
        // TODO Auto-generated method stub
        return new BooleanValue(evaluateConstraint(from, "x"));
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(Case caseInstance, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition, Task<?> task) {
        return new BooleanValue(evaluateConstraint(value, "x"));
    }

    @Override
    public boolean evaluateItemControl(PlanItem planItem, ConstraintDefinition ruleDefinition) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean evaluateIfPart(Criterion criterion, IfPartDefinition ifPartDefinition) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean evaluateApplicabilityRule(PlanItem containingPlanItem, DiscretionaryItemDefinition discretionaryItemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        // TODO Auto-generated method stub
        return false;
    }
}
