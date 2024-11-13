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

package com.casefabric.cmmn.definition.parameter;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ExpressionDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class BindingRefinementDefinition extends ExpressionDefinition {

    public BindingRefinementDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    public BindingOperation getRefinementOperation() {
        // Replace dashes --> Reference-Indexed and Update-Indexed, Replace-Indexed become as the enum requires them
        String body = (getBody() != null ? getBody().trim() : "").replace("-", "");
        for (BindingOperation operation : BindingOperation.values()) {
            if (operation.toString().equalsIgnoreCase(body)) {
                return operation;
            }
        }
        return BindingOperation.None;
    }
}
