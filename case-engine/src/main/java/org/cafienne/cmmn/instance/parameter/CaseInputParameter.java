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
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.json.Value;

/**
 * CaseInputParameters are passed upon Case creation. They are then bound to the case file (possibly triggering sentries in the case).
 */
public class CaseInputParameter extends Parameter<InputParameterDefinition> {
    public CaseInputParameter(InputParameterDefinition definition, Case caseInstance, Value<?> value) {
        super(definition, caseInstance, value);
        // Now do the binding to the case file, if it is defined
        if (hasBinding()) {
            CaseFileItem item = getBinding().getPath().resolve(getCaseInstance());
            // Validate proper types
            item.getDefinition().validatePropertyTypes(value);

            addDebugInfo(() -> "Binding parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "] (transition -> Create)");
            item.createContent(value);
        }
    }
}
