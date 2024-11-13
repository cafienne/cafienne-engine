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

import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Parameter;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;

/**
 * CaseOutputParameters are bound to the case file. They are filled at the moment the CasePlan completes.
 */
public class CaseOutputParameter extends Parameter<OutputParameterDefinition> {
    public CaseOutputParameter(OutputParameterDefinition definition, Case caseInstance) {
        super(definition, caseInstance, null);

        // If we have a binding defined, link this parameter to the case file via that binding
        // perhaps generate a debug statement if a case output parameter does not bind to case file?
        if (hasBinding()) {
            CaseFileItem item = getBinding().getPath().resolve(getCaseInstance());
            this.value = item.getValue();
        }
    }
}
