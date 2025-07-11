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

package org.cafienne.engine.cmmn.instance;

import org.cafienne.engine.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.engine.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.json.Value;

import java.io.Serializable;


public class Parameter<T extends ParameterDefinition> extends CMMNElement<T> implements Serializable {
    protected Value<?> value; // Default value is Null.

    protected Parameter(T definition, Case caseInstance, Value<?> value) {
        super(caseInstance, definition);
        this.value = value == null ? Value.NULL : value;
    }

    protected boolean hasBinding() {
        return getDefinition().getBinding() != null;
    }

    protected CaseFileItemDefinition getBinding() {
        return getDefinition().getBinding();
    }

    public String getName() {
        return getDefinition().getName();
    }

    @Override
    public String toString() {
        return getName() + " : " + value;
    }

    /**
     * Returns the current value of the parameter. If the parameter is bound to a case file item, the value of the case file item is used.
     * Otherwise, the value is stored and retrieved from within the parameter itself.
     * @return
     */
    public Value<?> getValue() {
        return value;
    }
}
