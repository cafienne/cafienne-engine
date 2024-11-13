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

import com.casefabric.cmmn.expression.InvalidExpressionException;
import com.casefabric.cmmn.expression.spel.api.APIObject;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.casefile.CaseFileItem;
import com.casefabric.cmmn.instance.casefile.CaseFileItemArray;

/**
 */
public class CaseFileItemAPI extends APIObject<Case> {
    protected final CaseFileItem item;
    protected final CaseFileItemAPI parent;

    public CaseFileItemAPI(CaseFileItem item) {
        super(item.getCaseInstance());
        this.item = item;
        parent = item.getParent() == null ? null : new CaseFileItemAPI(item.getParent());
        addPropertyReader("index", () -> item.getIndex());
        addPropertyReader("value", this::getValue);
        addPropertyReader("container", () -> new CaseFileItemAPI(item.getContainer()));
        addPropertyReader("current", () -> new CaseFileItemAPI(item.getCurrent()));
        addPropertyReader("parent", () -> parent);
    }

    public Object get(int index) {
        if (item.getContainer().isArray()) {
            CaseFileItemArray array = item.getContainer();
            return new CaseFileItemAPI(array.get(index)).getValue();
        } else {
            throw new InvalidExpressionException("Cannot read index " + index + " from non-array case file item " + this);
        }
    }

    public Object getValue() {
        // Note: valueAPI.getValue may return the actual primitive value itself.
        return new ValueAPI(item).getValue();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"[" + item.getPath() + "]";
    }
}
