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

            if (item.getState().isNull()) {
                addDebugInfo(() -> "Binding parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "] (transition -> Create)");
                item.createContent(value);
            } else {
                addDebugInfo(() -> "Binding parameter '" + getDefinition().getName() + "' to CaseFileItem[" + item.getPath() + "] (transition -> Replace) - This typically happens during reactivation of the case");
                item.replaceContent(value);
            }

        }
    }
}
