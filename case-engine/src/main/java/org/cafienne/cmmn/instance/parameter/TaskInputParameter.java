/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.parameter;

import org.cafienne.json.StringValue;
import org.cafienne.cmmn.definition.parameter.BindingOperation;
import org.cafienne.cmmn.definition.parameter.BindingRefinementDefinition;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;

/**
 * TaskInputParameter is specific from other parameters, in that its value is typically bound to the case file.
 * That is, if a {@link Task} assigns input parameters, the value of that parameter is typically retrieved from the case file.
 */
public class TaskInputParameter extends TaskParameter<InputParameterDefinition> {
    public TaskInputParameter(InputParameterDefinition definition, Task task) {
        // TaskInputParameters get a value when the task is activated, not when the task is instantiated
        super(definition, task, null);
        // If we have a binding defined, link this parameter to the case file via that binding
        if (binding != null) {
            bindCaseFileToTaskInputParameter();
        }
    }

    /**
     * Binds the value of the task input parameter to the case file, if a binding is defined
     */
    private void bindCaseFileToTaskInputParameter() {
        // If we have a binding defined, link this parameter to the case file via that binding, otherwise just return
        if (binding == null) {
            return;
        }
        CaseFileItem item = binding.getPath().resolve(getCaseInstance());

        // Old default behavior: we're navigating to the 'CURRENT' case file item. That is, for array type of case file item,
        //  this will lead to the item that is most recently modified.
        value = item.getCurrent().getValue();

        // New behavior: based on a specific string in the refinement we decide to pass the case file item by value or by reference
        //  and some additional logic for array types of case file item.
        BindingRefinementDefinition refinement = getDefinition().getBindingRefinement();
        if (refinement != null) {
            BindingOperation operation = refinement.getRefinementOperation();
            addDebugInfo(() -> {
                if (refinement == null || operation == BindingOperation.None) {
                    return "Binding input parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "] is done with default operation " + operation;
                } else {
                    return "Binding input parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "] is done with operation " + operation;
                }
            });

            switch (operation) {
                case Indexed: {
                    if (item.isArray()) {
                        value = item.getArrayElement(task.getRepeatIndex()).getValue();
                    } else {
                        addDebugInfo(() -> "Unexpected task input binding operation '" + operation + "' for parameter '" + getDefinition().getName() + "' because case file item is not an array; passing plain value of the item");
                        value = item.getValue();
                    }
                    break;
                }
                case List: {
                    if (! item.isArray()) {
                        addDebugInfo(() -> "Unexpected task input binding operation '" + operation + "' for parameter '" + getDefinition().getName() + "' because case file item is not an array; passing plain value of the item");
                    }
                    value = item.getValue();
                    break;
                }
                case Current: {
                    value = item.getCurrent().getValue();
                    break;
                }
                case Reference: {
                    value = new StringValue(item.getPath().toString());
                    break;
                }
                case ReferenceIndexed: {
                    if (item.isArray()) {
                        value = new StringValue(item.getArrayElement(task.getRepeatIndex()).getPath().toString());
                    } else {
                        addDebugInfo(() -> "Unexpected task input binding operation '" + operation + "' for parameter '" + getDefinition().getName() + "' because case file item is not an array; passing plain reference of the item");
                        value = new StringValue(item.getPath().toString());
                    }
                    break;
                }
            }
        }
    }

}
