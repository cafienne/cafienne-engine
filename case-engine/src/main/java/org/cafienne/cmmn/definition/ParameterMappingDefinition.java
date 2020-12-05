/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import java.util.Map;

import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.definition.task.TaskImplementationContract;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.w3c.dom.Element;

public class ParameterMappingDefinition extends CMMNElementDefinition {
    private final TaskDefinition<?> taskDefinition;
    private final String sourceRef;
    private final String targetRef;
    private final ExpressionDefinition transformation;
    private ParameterDefinition source;
    private ParameterDefinition target;
    private boolean isInputMapping;

    public ParameterMappingDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.taskDefinition = findTask(parentElement);
        this.sourceRef = parseAttribute("sourceRef", true);
        this.targetRef = parseAttribute("targetRef", true);
        this.transformation = parse("transformation", ExpressionDefinition.class, false);
    }
    
    /**
     * Recursively searches the CMMN definition tree until it finds a Task.
     * This is done because the ParameterMapping is defined inside CaseTask and inside ProcessTask, but for HumanTask it is defined in the 
     * custom implementation tag which is not a direct child of the Task definition itself.
     * @param parentElement
     * @return
     */
    private TaskDefinition<?> findTask(CMMNElementDefinition parentElement) {
        if (parentElement == null) {
            getModelDefinition().addDefinitionError("The parameter mapping '" + getId()+"' has no surrounding Task element, and can therefore not be used");
            return null;
        }
        if (parentElement instanceof TaskDefinition) {
            return (TaskDefinition<?>) parentElement;
        }
        return findTask(parentElement.getParentElement());
    }

    @Override
    public String getContextDescription() {
        String parentType = getParentElement().getType();
        String parentId = getParentElement().getId();
        // This will return something like "The parametermapping in HumanTask 'abc'
        return "The parameter mapping in " + parentType + " '" + parentId + "'";
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        TaskImplementationContract taskImplementationDefinition = taskDefinition.getImplementationDefinition();
        if (taskImplementationDefinition == null) {
            // If implementation definition is not found, a reference error will be added from the task itself.
            //  No need to throw an error here, simply return and do not continue (as it leads to NullPointerException)
            return;
        }
        source = findParameter(taskDefinition.getInputParameters(), sourceRef);
        if (source == null) {
            isInputMapping = false;
            source = findParameter(taskImplementationDefinition.getOutputParameters(), sourceRef);
            target = findParameter(taskDefinition.getOutputParameters(), targetRef);
            if (source == null) {
                String msg = "The parameter mapping '" + getDescriptionForError() + "' has sourceRef " + sourceRef + ", but that output parameter is not found in "+taskDefinition.getImplementationDefinition().getId();
                getModelDefinition().addReferenceError(msg);
                if (target == null) {
                    if (findParameter(taskImplementationDefinition.getInputParameters(), targetRef) != null) {
                        // Well, apparently the parameter mapping is reversed, and the source ref is not found on the task
                        getModelDefinition().addReferenceError("The parameter mapping '" + getDescriptionForError() + "' has invalid sourceRef " + sourceRef + ", because the task does not have this output parameter");
                    }
                }
            } else if (target == null) {
                String msg = "The parameter mapping '" + getDescriptionForError() + "' has targetRef " + targetRef + ", but the task does not have this output parameter";
                getModelDefinition().addReferenceError(msg);
            }
        } else {
            // Source exists in the input parameters; hence target must also be found there, and it is an input mapping
            isInputMapping = true;
            target = findParameter(taskImplementationDefinition.getInputParameters(), targetRef);
            if (target == null) {
                String msg = "The input parameter mapping '" + getDescriptionForError() + "' has targetRef " + targetRef + ", but that input parameter is not found in " + taskDefinition.getImplementationDefinition().getId();
                getModelDefinition().addReferenceError(msg);
            }
        }
    }

    private String getTaskDefinitionName() {
        CMMNElementDefinition parent = getParentElement();
        while (parent!=null && ! (parent instanceof TaskDefinition)) {
            parent = parent.getParentElement();
        }
        if (parent != null) {
            return parent.getName();
        }
        return "without Task";
    }

    private String getDescriptionForError() {
        String taskName = getTaskDefinitionName();
        String caseName = getCaseDefinition().getName();
        return getId() +"' in task '"+taskName+"' in case '" + caseName;
    }

    /**
     * Helper to identify parameter from a collection (supporting reference both by name and by id)
     * @param definitions
     * @param identifier
     * @return
     */
    private <T extends ParameterDefinition> ParameterDefinition findParameter(Map<String, T> definitions, String identifier) {
        ParameterDefinition p = definitions.values().stream().filter(i -> {
            String name = i.getName();
            String id = i.getId();
            return name.equals(identifier) || id.equals(identifier);
        }).findFirst().orElse(null);
        return p;
    }
    
    /**
     * Returns the source of the mapping.
     *
     * @return
     */
    public ParameterDefinition getSource() {
        return source;
    }

    /**
     * Returns the target parameter of the mapping.
     *
     * @return
     */
    public ParameterDefinition getTarget() {
        return target;
    }

    /**
     * Returns the (optional) transformation to be executed during the mapping.
     *
     * @return
     */
    public ExpressionDefinition getTransformation() {
        return transformation;
    }

    /**
     * Indicates whether this mapping is to be executed before or after the Task is executed. This is determined automagically based on the parameter references. They must either both refer to input
     * parameters or to output parameters. Also, in case the sourceRef and targetRef refer to input parameters this means the mapping is to be executed before the task is executed. If they refer to
     * output parameters, the mapping is executed after the task has been completed.
     *
     * @return
     */
    public boolean isInputParameterMapping() {
        return isInputMapping;
    }

    /**
     * Invokes the transformation logic on the sourceParameter (within the task context) and returns the outcome
     *
     * @param task
     * @param sourceParameter
     * @return The value of the "raw" output parameter for the "content" of the task (subcase or subprocess)
     */
    public Value<?> transformInput(Task<?> task, TaskInputParameter sourceParameter) {
        Value<?> targetValue = sourceParameter.getValue();
        if (transformation != null && !transformation.getBody().isEmpty()) {
            targetValue = transformation.getEvaluator().evaluateInputParameterTransformation(task.getCaseInstance(), sourceParameter, target, task);
        }
        return targetValue;
    }

    /**
     * Transforms the "raw" output of a parameter into a TaskOutputParameter, by invoking the transformation logic of this mapping.
     *
     * @param task
     * @param value The raw output parameter value after the "content" of the task has been executed, i.e., the output of the subprocess or subcase
     * @return The mapped value
     */
    public Value<?> transformOutput(Task<?> task, Value<?> value) {
        Value<?> targetValue = value;
        if (transformation != null && !transformation.getBody().isEmpty()) {
            targetValue = transformation.getEvaluator().evaluateOutputParameterTransformation(task.getCaseInstance(), value, source, target, task);
        }
        return targetValue;
    }
}
