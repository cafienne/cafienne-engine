/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ExpressionDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.*;

public class MappingDefinition extends ExpressionDefinition {
    private final String sourceReferences;
    private final String target;
    private final Map<String, SourceDefinition> sources = new HashMap();
    private Result result;

    public MappingDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.sourceReferences = parseAttribute("source", true);
        this.target = parseAttribute("target", true);
    }

    protected void checkDependencies(CalculationDefinition parent) {
        StringTokenizer references = new StringTokenizer(this.sourceReferences, ",");
        while (references.hasMoreTokens()) {
            // Only check trimmed, non-blank source references
            String sourceReference = references.nextToken().trim();
            if (!sourceReference.isBlank()) {
                SourceDefinition inputSource = parent.getSource(sourceReference);
                // Make sure source is defined
                if (inputSource == null) {
                    this.getProcessDefinition().addDefinitionError("Cannot find source '" + sourceReference + "'");
                }
                // Make sure source is not dependent on us too
                if (inputSource.hasDependency(this)) {
                    this.getProcessDefinition().addDefinitionError("Source '" + sourceReference + "' has recursive reference to '" + target + "'");
                }
                // That's a valid source then, add it to our incoming dependencies
                sources.put(sourceReference, inputSource);
            }
        }
    }

    boolean hasDependency(MappingDefinition mappingDefinition) {
        return this.sources.containsKey(mappingDefinition.target);
    }

    String getTarget() {
        return target;
    }

    /**
     * Return the outcome of the calculation, if required first calculate it.

     * @param calculation
     * @return
     */
    Value getValue(Calculation calculation) {
        if (result == null) {
            result = calculateResult(calculation);
        }
        return result.value;
    }

    private Result calculateResult(Calculation calculation) {
        // First make a map with the incoming dependencies by name
        ValueMap sourceMap = new ValueMap();
        sources.forEach((name, source) -> sourceMap.put(name, source.getValue(calculation).cloneValueNode()));

        // If there is an expression, execute it on the incoming values, otherwise just return the incoming values
        if (getBody().isEmpty()) {
            return new Result(sourceMap);
        } else {
            return new Result(getEvaluator().runCalculationStep(calculation, sourceMap));
        }
    }

    class Result {
        private final Value value;
        Result(Value value) {
            this.value = value;
        }
    }
}
