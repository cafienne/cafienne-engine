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

package com.casefabric.cmmn.expression.spel;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.expression.spel.Evaluator;
import com.casefabric.cmmn.expression.spel.api.APIRootObject;
import org.springframework.expression.Expression;

public class ParameterEvaluator extends Evaluator {
    private final String parameterName;

    public ParameterEvaluator(CMMNElementDefinition definition, String source) {
        super(definition, source);
        this.parameterName = source;
    }

    @Override
    protected Expression parseExpression() {
        return null;
    }

    /**
     * Always returns a string representation of the parameter value, or null.
     */
    @Override
    public <T> T evaluate(APIRootObject<?> rootObject) {
        @SuppressWarnings("unchecked")
        T i_m_always_a_string = (T) returnValue(rootObject, () -> {
            Object value = rootObject.read(parameterName);
            if (value == null) return null;
            return String.valueOf(value);
        });
        return i_m_always_a_string;
    }
}
