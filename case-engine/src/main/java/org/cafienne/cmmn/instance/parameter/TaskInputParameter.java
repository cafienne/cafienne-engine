/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.parameter;

import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Parameter;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.casefile.Value;

/**
 * TaskInputParameter is specific from other parameters, in that its value is typically bound to the case file.
 * That is, if a {@link Task} assigns input parameters, the value of that parameter is typically retrieved from the case file.
 */
public class TaskInputParameter extends Parameter<InputParameterDefinition> {
    public TaskInputParameter(InputParameterDefinition definition, Case caseInstance) {
        super(definition, caseInstance, null);
    }

    @Override
    public Value<?> getValue() {
        super.bindCaseFileToParameter();
        return super.getValue();
    }
}
