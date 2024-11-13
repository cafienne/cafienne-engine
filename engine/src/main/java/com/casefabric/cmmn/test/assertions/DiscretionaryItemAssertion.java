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

package com.casefabric.cmmn.test.assertions;

import com.casefabric.cmmn.instance.PlanItemType;
import com.casefabric.cmmn.test.CaseTestCommand;
import com.casefabric.json.ValueMap;

/**
 * Some assertions for discretionary items.
 */
public class DiscretionaryItemAssertion extends ModelTestCommandAssertion{

    private final ValueMap item;

    DiscretionaryItemAssertion(CaseTestCommand command, ValueMap item) {
        super(command);
        this.item = item;
    }

    /**
     * Throws an exception if the discretionary item is of a different type than the expected one.
     *
     * @param expectedType
     */
    public void assertType(PlanItemType expectedType) {
        if (!getType().equals(expectedType)) {
            throw new AssertionError("Discretionary item is of type " + getType() + " instead of the expected type " + expectedType);
        }
    }

    /**
     * Returns the identifier of the DiscretionaryItem
     * @return
     */
    public String getDefinitionId() {
        return item.raw("definitionId");
    }

    public String getName() {
        return item.raw("name");
    }

    public PlanItemType getType() {
        return item.readEnum("type", PlanItemType.class);
    }

    public String getParentId() {
        return item.raw("parentId");
    }

    @Override
    public String toString() {
        return item.toString();
    }
}
