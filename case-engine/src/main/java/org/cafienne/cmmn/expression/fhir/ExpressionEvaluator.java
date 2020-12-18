/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.fhir;

import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.parser.FHIRParser;
import com.ibm.fhir.model.parser.exception.FHIRParserException;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.path.FHIRPathNode;
import com.ibm.fhir.path.FHIRPathParser;
import com.ibm.fhir.path.evaluator.FHIRPathEvaluator;
import com.ibm.fhir.path.exception.FHIRPathException;
import com.ibm.fhir.path.util.FHIRPathUtil;
import org.cafienne.akka.actor.serialization.json.Value;
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

import java.io.StringReader;
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

    private Collection<FHIRPathNode> evaluateExpression(Case caseInstance, ConstraintDefinition constraint, String contextDescription) {
        caseInstance.addDebugInfo(() -> contextDescription +": evaluating the expression " + expressionString);

        FHIRPathEvaluator ev = FHIRPathEvaluator.evaluator();
        try {
            CaseFileItem item = constraint.resolveContext(caseInstance);
            String json = item.getValue().toString();
            Resource fhirResourceInstance = FHIRParser.parser(Format.JSON).parse(new StringReader(json));

            FHIRPathEvaluator.EvaluationContext evaluationContext = new FHIRPathEvaluator.EvaluationContext(fhirResourceInstance);
            Collection<FHIRPathNode> result = FHIRPathEvaluator.evaluator().evaluate(evaluationContext, expressionString);
            System.out.println("REsult: " + result);
            return result;
        } catch (FHIRPathException e) {
            caseInstance.addDebugInfo(() -> "Failure in evaluating ifPart with expression "+ expressionString.trim(), e);
            throw new InvalidExpressionException("Could not evaluate " + expressionString + "\n" + e.getLocalizedMessage(), e);
        } catch (FHIRParserException e) {
            caseInstance.addDebugInfo(() -> "Failure in evaluating ifPart with expression "+ expressionString.trim(), e);
            throw new InvalidExpressionException("Could not evaluate " + expressionString + "\n" + e.getLocalizedMessage(), e);
        }
    }


    private boolean evaluateConstraint(Case caseInstance, ConstraintDefinition constraint, String contextDescription) {
        Collection<FHIRPathNode> result = evaluateExpression(caseInstance, constraint, contextDescription);
        System.out.println("FHIRPathUtil.isTrue(result): " + FHIRPathUtil.isTrue(result));
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

    @Override
    public Value<?> evaluateInputParameterTransformation(Case caseInstance, TaskInputParameter from, ParameterDefinition to, Task<?> task) {
        return Value.NULL;
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(Case caseInstance, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition, Task<?> task) {
        return Value.NULL;
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(ProcessTaskActor processTaskActor, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition) {
        return Value.NULL;
    }
}
