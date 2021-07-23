/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.parameter;

import org.cafienne.cmmn.definition.parameter.BindingOperation;
import org.cafienne.cmmn.definition.parameter.BindingRefinementDefinition;
import org.cafienne.cmmn.definition.parameter.TaskOutputParameterDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.TransitionDeniedException;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.json.Value;

/**
 * A TaskOutputParameter is created right before a task completes.
 * If its value is set (after it is mapped from the raw output of the task), it is bound to the case file.
 */
public class TaskOutputParameter extends TaskParameter<TaskOutputParameterDefinition> {
    public TaskOutputParameter(TaskOutputParameterDefinition definition, Task<?> task, Value<?> value) {
        super(definition, task, value);
    }

    public void validate() {
        if (!hasBinding()) {
            addDebugInfo(() -> "Parameter '" + getName() + "' has no case file binding binding");
            return;
        }
        CaseFileItem item = getBinding().getPath().resolve(getCaseInstance());
        addDebugInfo(() -> "Validating property types of a " + item.getName() + " against value ", value);
        item.getDefinition().validatePropertyTypes(value);

        if (item.getState() == State.Discarded) {
            addDebugInfo(() -> "Cannot bind parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "], since the item is in state Discarded");
            throw new TransitionDeniedException("Cannot bind parameter value, because case file item has been deleted");
        }
    }

    /**
     * Binding to case file must not be done upon task output validation, only upon task completion.
     */
    public void bind() {
        // If we have a binding defined, bind this parameter to the case file via that binding, otherwise just return
        if (!hasBinding()) {
            return;
        }
        CaseFileItem item = getBinding().getPath().resolve(getCaseInstance());

        // Spec says (table 5.3.4, page 36): "just trigger the proper transition, as that will be obvious." But is it?
        //  We have implemented specific type of BindingRefinement to make more predictable what to do

        switch (item.getState()) {
            case Discarded: {
                // This check is also done during task output parameter creation, nevertheless adding debug info warning and return with no further action
                addDebugInfo(() -> "Cannot bind parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "], since the item is in state Discarded");
                return;
            }
            case Null: {
                // In all cases we will try to create content if state is Null;
                //  If we are an array, then an item in the array is created. Else we ourselves are created
                addDebugInfo(() -> "Binding parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "] will create new content (transition -> Create)");
                item.createContent(value);
                break;
            }
            case Available: {
                BindingRefinementDefinition refinement = getDefinition().getBindingRefinement();
                BindingOperation operation = refinement != null ? refinement.getRefinementOperation() : item.isArray() ? BindingOperation.Add : BindingOperation.Update;
                addDebugInfo(() -> {
                    if (refinement == null || operation == BindingOperation.None) {
                        return "Binding parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "] is done with default operation " + operation;
                    } else {
                        return "Binding parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "] is done with operation " + operation;
                    }
                });

                switch (operation) {
                    case Add: {
                        if (item.isArray()) {
                            item.createContent(value);
                        } else {
                            addDebugInfo(() -> "Unexpected task output operation '" + operation + "' on value of parameter '" + getDefinition().getName() + "' because case file item already exists; updating content instead");
                            item.updateContent(value);
                        }
                        break;
                    }
                    case Replace: {
                        item.replaceContent(value);
                        break;
                    }
                    case Update: {
                        item.updateContent(value);
                        break;
                    }
                    case ReplaceIndexed: {
                        if (item.isArray()) {
                            item.getArrayElement(task.getRepeatIndex()).replaceContent(value);
                        } else {
                            addDebugInfo(() -> "Unexpected task output operation '" + operation + "' on value of parameter '" + getDefinition().getName() + "' because case file item already exists; updating content instead");
                            item.replaceContent(value);
                        }
                        break;
                    }
                    case UpdateIndexed: {
                        if (item.isArray()) {
                            item.getArrayElement(task.getRepeatIndex()).updateContent(value);
                        } else {
                            addDebugInfo(() -> "Unexpected task output operation '" + operation + "' on value of parameter '" + getDefinition().getName() + "' because case file item already exists; updating content instead");
                            item.updateContent(value);
                        }
                        break;
                    }
                }
            }
        }
    }
}
