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

package org.cafienne.engine.processtask.definition;

import org.cafienne.engine.cmmn.definition.CMMNElementDefinition;
import org.cafienne.engine.cmmn.definition.ExpressionDefinition;
import org.cafienne.engine.cmmn.definition.ModelDefinition;
import org.cafienne.engine.cmmn.definition.ParameterMappingDefinition;
import org.cafienne.engine.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.engine.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

/**
 * This class represents the mapping between Process to its implementation.
 * It is similar to {@link ParameterMappingDefinition}, but it is proprietary for the engine Process implementations.
 * Currently it only supports mapping output parameters of the implementation to the Process.
 */
public class SubProcessOutputMappingDefinition extends CMMNElementDefinition {
    private final String sourceRef;
    private final String targetRef;
    private final ExpressionDefinition transformation;
    private ParameterDefinition source;
    private ParameterDefinition target;

    public SubProcessOutputMappingDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.sourceRef = parseAttribute("sourceRef", true);
        this.targetRef = parseAttribute("targetRef", true);
        this.transformation = parse("transformation", ExpressionDefinition.class, false);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();

        target = getProcessDefinition().getOutputParameters().get(targetRef);
        if (target == null) {
            getModelDefinition().addReferenceError("Invalid mapping " + getId() + ": target parameter " + targetRef + " is missing");
        }

        SubProcessDefinition spd = getParentElement();
        source = spd.getRawOutputParameters().get(sourceRef);
        if (source == null) {
            getModelDefinition().addReferenceError("Invalid mapping in process definition " + spd.getParentElement().getId() + ": source parameter " + sourceRef + " cannot be used; use one of " + spd.getRawOutputParameterNames());
        }
    }

    @Override
    public String getContextDescription() {
        SubProcessDefinition spd = getParentElement();
        String parentId = spd.getParentElement().getId();
        return "The mapping in process with id '" + parentId + "'";
    }

    /**
     * This method to transform the raw output values of a SubProcess implementation to Process output parameters
     *
     * @param processTaskActor
     * @param rawOutputParameters
     * @return transformed output value
     */
    public Value<?> transformOutput(ProcessTaskActor processTaskActor, ValueMap rawOutputParameters) {
        Value<?> targetValue = rawOutputParameters.get(sourceRef); // By default, the target value is the same as the source value
        if (transformation != null && !transformation.getBody().isEmpty()) {
            // But if there is a transformation defined, then we will evaluate the expression on the source value
            targetValue = transformation.getEvaluator().evaluateOutputParameterTransformation(processTaskActor, targetValue, source, target);
        }
        return targetValue;
    }

    public ParameterDefinition getTarget() {
        return target;
    }

    public ParameterDefinition getSource() {
        return source;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameMapping);
    }

    public boolean sameMapping(SubProcessOutputMappingDefinition other) {
        return same(transformation, other.transformation)
                && same(source, other.source)
                && same(target, other.target);
    }
}
