/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.task;

import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;

import java.util.Map;

/**
 * In the CMMN 1.0 specification, {@link CaseTask} and {@link ProcessTask} have an implementation, whereas {@link HumanTask} has not.
 * In practice, however, {@HumanTask} is typically associated with a workflow implementation, providing for it's own lifecycle.
 * The engine also provides an extension on top of CMMN to enable workflow.
 * 
 * With that, every {@link Task} has an implementation, and there is mapping and binding of parameters to each implementation.
 * The engine has abstracted this binding in the {@link TaskImplementationContract}, describing the inputs and outputs of the implementation.
 * See also {@link CaseDefinition}, {@link ProcessDefinition} & {@link WorkflowTaskDefinition}
 */
public interface TaskImplementationContract {
	/**
	 * returns the id of the contract
	 * @return
	 */
	String getId();

	/**
	 * This method should return a map of input parameter definitions of actual task implementation
	 * @return a map of input parameter definitions of actual task implementation
	 */
	Map<String, InputParameterDefinition> getInputParameters();

	/**
	 * This method should return a map of output parameter definitions of actual task implementation
	 * @return a map of output parameter definitions of actual task implementation
	 */
	Map<String, OutputParameterDefinition> getOutputParameters();
}
