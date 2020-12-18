/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.fhir;

import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.generator.FHIRGenerator;
import com.ibm.fhir.model.generator.exception.FHIRGeneratorException;
import com.ibm.fhir.model.parser.FHIRParser;
import com.ibm.fhir.model.parser.exception.FHIRParserException;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.util.JsonSupport;
import com.ibm.fhir.model.visitor.Visitable;
import com.ibm.fhir.path.FHIRPathNode;
import com.ibm.fhir.path.FHIRPathParser;
import com.ibm.fhir.path.FHIRPathSystemValue;
import com.ibm.fhir.path.evaluator.FHIRPathEvaluator;
import com.ibm.fhir.path.exception.FHIRPathException;
import com.ibm.fhir.path.util.FHIRPathUtil;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.serialization.json.*;
import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.cmmn.expression.CMMNExpressionEvaluator;
import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.String;
import java.util.Collection;

import static com.ibm.fhir.model.type.code.NarrativeStatus.string;

public class ExpressionEvaluator implements CMMNExpressionEvaluator {
    private final String expressionString;
    private final ExpressionDefinition expressionDefinition;
    private final FHIRPathParser.ExpressionContext context;

    public ExpressionEvaluator(ExpressionDefinition expressionDefinition) {
        this.expressionString = expressionDefinition.getBody();
        this.expressionDefinition = expressionDefinition;
        this.context = parseExpression();
    }

    private FHIRPathParser.ExpressionContext parseExpression() {
        try {
            return FHIRPathUtil.compile(expressionString);
        } catch (Exception spe) {
            expressionDefinition.getModelDefinition().addDefinitionError(expressionDefinition.getContextDescription() + " has an invalid expression:\n" + spe.getMessage());
            return null;
        }
    }

    private Collection<FHIRPathNode> evaluateExpression(ModelActor modelInstance, Value context, String contextDescription) {
        modelInstance.addDebugInfo(() -> contextDescription + ": evaluating the expression " + expressionString);

        try {
            String json = context.toString();
            Resource fhirResourceInstance = FHIRParser.parser(Format.JSON).parse(new StringReader(json));

            FHIRPathEvaluator.EvaluationContext evaluationContext = new FHIRPathEvaluator.EvaluationContext(fhirResourceInstance);
            Collection<FHIRPathNode> result = FHIRPathEvaluator.evaluator().evaluate(evaluationContext, expressionString);
            return result;
        } catch (FHIRPathException e) {
            modelInstance.addDebugInfo(() -> "Failure in evaluating ifPart with expression " + expressionString.trim(), e);
            throw new InvalidExpressionException("Could not evaluate " + expressionString + "\n" + e.getLocalizedMessage(), e);
        } catch (FHIRParserException e) {
            modelInstance.addDebugInfo(() -> "Failure in evaluating ifPart with expression " + expressionString.trim(), e);
            throw new InvalidExpressionException("Could not evaluate " + expressionString + "\n" + e.getLocalizedMessage(), e);
        }
    }


    private boolean evaluateConstraint(Case caseInstance, ConstraintDefinition constraint, String contextDescription) {
        CaseFileItem item = constraint.resolveContext(caseInstance);
        Collection<FHIRPathNode> result = evaluateExpression(caseInstance, item.getValue(), contextDescription);
        return FHIRPathUtil.isTrue(result);
    }

    @Override
    public boolean evaluateItemControl(PlanItem planItem, ConstraintDefinition constraint) {
        return evaluateConstraint(planItem.getCaseInstance(), constraint, constraint.getContextDescription());
    }

    @Override
    public boolean evaluateIfPart(Criterion criterion, IfPartDefinition ifPartDefinition) {
        return evaluateConstraint(criterion.getCaseInstance(), ifPartDefinition, "ifPart in sentry");
    }

    @Override
    public boolean evaluateApplicabilityRule(PlanItem containingPlanItem, DiscretionaryItemDefinition discretionaryItemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        String description = "applicability rule '" + ruleDefinition.getName() + "' for discretionary item " + discretionaryItemDefinition;
        return evaluateConstraint(containingPlanItem.getCaseInstance(), ruleDefinition, description);
    }

    private Value<?> evaluateToValueObject(ModelActor actor, Value context, String contextDescription) {
        Collection<FHIRPathNode> result = evaluateExpression(actor, context, contextDescription);
        Value v;
        if (result.size() > 1) {
            ValueList list = new ValueList();
            result.forEach(node -> list.add(nodeConverter(node)));
            v = list;
        } else {
            v = nodeConverter(result.stream().findFirst().orElse(null));
        }
//
//        System.out.println("REsult with type " + v.getClass().getSimpleName() + ": " + v);

        return v;
    }

    private Value nodeConverter(FHIRPathNode node) {
        if (node == null) {
            // TODO: figure out how to make this happen. Will expression always result in at least 1 node???
            System.out.println("Returning a nul node");
            return Value.NULL;
        }
        if (node.isElementNode() || node.isResourceNode()) {
            if (node.isElementNode()) {
                System.out.println("Converting element node of type " + node.asElementNode().type());
            } else {
                System.out.println("Converting resource node of type " + node.asResourceNode().type());
            }
            Visitable v = node.isElementNode() ? node.asElementNode().element() : node.asResourceNode().resource();
            StringWriter writer = new StringWriter();
            try {
                FHIRGenerator.generator(Format.JSON).generate(v, writer);
                return JSONReader.parse(writer.toString());
            } catch (FHIRGeneratorException | JSONParseFailure | IOException wePromiseWeWillNotOccurOrOurUnderlyingLibrariesAreReallyMissingThePointOfJson) {
                wePromiseWeWillNotOccurOrOurUnderlyingLibrariesAreReallyMissingThePointOfJson.printStackTrace();
            }
        } else if (node.isSystemValue()) {
            return convertSystemValue(node.asSystemValue());
        } else {
            System.out.println("Cannot (yet) convert nodes of type " + node.getClass().getName());
        }
        return Value.NULL;
    }

    private Value convertSystemValue(FHIRPathSystemValue node) {
        if (node.isStringValue()) {
            return new StringValue(node.asStringValue().string());
        } else if (node.isBooleanValue()) {
            return new BooleanValue(node.asBooleanValue()._boolean());
        } else if (node.isNumberValue()) {
            return new LongValue(node.asNumberValue().integer());
        } else if (node.isTemporalValue()) {
            System.out.println("Cannot (yet) convert nodes of type " + node.getClass().getName());
            node.asTemporalValue().temporal();
        }
        System.out.println("Cannot (yet) convert nodes of type " + node.getClass().getName());
        return Value.NULL;
    }

    @Override
    public Value<?> evaluateInputParameterTransformation(Case caseInstance, TaskInputParameter from, ParameterDefinition to, Task<?> task) {
        return evaluateToValueObject(caseInstance, from.getValue(), "Mapping to task input parameter '" + to.getName() + "'");
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(Case caseInstance, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition, Task<?> task) {
        return evaluateToValueObject(caseInstance, value, "Mapping to task output parameter '" + targetOutputParameterDefinition.getName() + "'");
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(ProcessTaskActor processTaskActor, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition) {
        return evaluateToValueObject(processTaskActor, value, "Mapping to implementation output parameter '" + targetOutputParameterDefinition.getName() + "'");
    }
}
