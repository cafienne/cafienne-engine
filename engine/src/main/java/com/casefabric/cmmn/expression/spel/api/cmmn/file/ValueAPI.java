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

package com.casefabric.cmmn.expression.spel.api.cmmn.file;

import com.casefabric.cmmn.expression.spel.SpelPropertyValueProvider;
import com.casefabric.cmmn.expression.spel.api.APIObject;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.casefile.CaseFileItem;
import com.casefabric.json.Value;

/**
 */
public class ValueAPI extends APIObject<Case> implements SpelPropertyValueProvider {
    private final Value<?> value;

    public ValueAPI(CaseFileItem item) {
        super(item.getCaseInstance());
        this.value = item.getValue();
    }

    @Override
    public Object getValue() {
        if (value.isPrimitive()) {
            return value.getValue();
        } else {
            return value;
        }
    }
}
