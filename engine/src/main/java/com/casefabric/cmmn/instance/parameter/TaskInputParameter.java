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

package com.casefabric.cmmn.instance.parameter;

import com.casefabric.cmmn.definition.parameter.BindingOperation;
import com.casefabric.cmmn.definition.parameter.BindingRefinementDefinition;
import com.casefabric.cmmn.definition.parameter.InputParameterDefinition;
import com.casefabric.cmmn.instance.Task;
import com.casefabric.cmmn.instance.casefile.CaseFileItem;
import com.casefabric.json.StringValue;

/**
 * TaskInputParameter is specific from other parameters, in that its value is typically bound to the case file.
 * That is, if a {@link Task} assigns input parameters, the value of that parameter is typically retrieved from the case file.
 */
public class TaskInputParameter extends TaskParameter<InputParameterDefinition> {
    public TaskInputParameter(InputParameterDefinition definition, Task<?> task) {
        // TaskInputParameters get a value when the task is activated, not when the task is instantiated
        super(definition, task, null);
        // If we have a binding defined, link this parameter to the case file via that binding
        if (hasBinding()) {
            bindCaseFileToTaskInputParameter();
        }
    }

    /**
     * Binds the value of the task input parameter to the case file, if a binding is defined
     */
    private void bindCaseFileToTaskInputParameter() {
        // If we have a binding defined, link this parameter to the case file via that binding, otherwise just return
        CaseFileItem item = getBinding().getPath().resolve(getCaseInstance());

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
