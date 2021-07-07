/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.json.Value;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;

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
