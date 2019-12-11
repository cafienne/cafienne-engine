/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.parameter;

import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Parameter;
import org.cafienne.cmmn.instance.casefile.Value;

/**
 * CaseOutputParameters are bound to the case file. They are filled at the moment the CasePlan completes.
 */
public class CaseOutputParameter extends Parameter<OutputParameterDefinition> {
    public CaseOutputParameter(OutputParameterDefinition definition, Case caseInstance) {
        super(definition, caseInstance);
    }

    @Override
    public Value<?> getValue() {
        super.bindCaseFileToParameter();
        return super.getValue();
    }
}
