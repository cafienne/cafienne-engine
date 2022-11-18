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

package org.cafienne.cmmn.definition.task;

import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
import org.cafienne.cmmn.instance.Task;

import java.util.Map;

/**
 * In the CMMN 1.0 specification, {@link org.cafienne.cmmn.instance.task.cmmn.CaseTask}
 * and {@link org.cafienne.cmmn.instance.task.process.ProcessTask} have an implementation,
 * whereas {@link org.cafienne.cmmn.instance.task.humantask.HumanTask} has not.
 * In practice, however, {@HumanTask} is typically associated with a workflow implementation, providing for it's own lifecycle.
 * The engine also provides an extension on top of CMMN to enable workflow.
 * <p>
 * With that, every {@link Task} has an implementation, and there is mapping and binding of parameters to each implementation.
 * The engine has abstracted this binding in the {@link TaskImplementationContract}, describing the inputs and outputs of the implementation.
 * See also {@link org.cafienne.cmmn.definition.CaseDefinition}, {@link org.cafienne.processtask.definition.ProcessDefinition} & {@link WorkflowTaskDefinition}
 */
public interface TaskImplementationContract {
    /**
     * returns the id of the contract
     *
     * @return
     */
    String getId();

    /**
     * This method should return a map of input parameter definitions of actual task implementation
     *
     * @return a map of input parameter definitions of actual task implementation
     */
    Map<String, InputParameterDefinition> getInputParameters();

    /**
     * This method should return a map of output parameter definitions of actual task implementation
     *
     * @return a map of output parameter definitions of actual task implementation
     */
    Map<String, OutputParameterDefinition> getOutputParameters();
}
