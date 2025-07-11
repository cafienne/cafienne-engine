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

package org.cafienne.engine.cmmn.test.assertions;

import org.cafienne.engine.cmmn.actorapi.response.GetDiscretionaryItemsResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueList;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Some assertions around discretionary items.
 * Note: currently this does not maintain the nested structure that planning tables may have.
 */
public class PlanningTableAssertion extends ModelTestCommandAssertion {

    private final Collection<DiscretionaryItemAssertion> discretionaries = new ArrayList<>();
    private final GetDiscretionaryItemsResponse response;
    private ValueList items = new ValueList();

    public PlanningTableAssertion(CaseAssertion response) {
        super(response.getTestCommand());
        this.response = response.getTestCommand().getActualResponse();
        this.response.toJson().withArray(Fields.discretionaryItems).forEach(value -> discretionaries.add(new DiscretionaryItemAssertion(response.getTestCommand(), value.asMap())));
    }

    @Override
    public String toString() {
        return response.toString();
    }

    /**
     * Asserts that there are no discretionary items in the planning table
     */
    public void assertNoItems() {
        if (!discretionaries.isEmpty()) {
            throw new AssertionError("Expect no discretionary items, but found " + items.size());
        }
    }

    /**
     * Asserts that there is more than 1 discretionary item in the planning table
     */
    public void assertItems() {
        if (discretionaries.isEmpty()) {
            throw new AssertionError("Expect discretionary items, but found none");
        }
    }

    /**
     * Asserts that the set of discretionary items contains the specified names
     *
     * @param identifiers
     * @return
     */
    public void assertItems(String... identifiers) {
        for (int i = 0; i < identifiers.length; i++) {
            String identifier = identifiers[i];
            this.assertItem(identifier);
        }
    }

    /**
     * Asserts that a discretionary item with the specified name is available in the planning table
     *
     * @param identifier
     * @return
     */
    public DiscretionaryItemAssertion assertItem(String identifier) {
        DiscretionaryItemAssertion item = getItem(identifier);
        if (item != null) {
            return item;
        }
        throw new AssertionError("A discretionary item '" + identifier + "' cannot be found in the planning table");
    }

    /**
     * Asserts that the planning table does not contain an item with the specified name. Can be used to check that an item is no longer available.
     *
     * @param name
     */
    public void assertNoItem(String name) {
        DiscretionaryItemAssertion item = getItem(name);
        if (item != null) {
            throw new AssertionError("A discretionary item '" + name + "' is found in the planning table, but it is not supposed to be there");
        }
    }

    private DiscretionaryItemAssertion getItem(String identifier) {
        for (DiscretionaryItemAssertion discretionaryItem : discretionaries) {
            if (discretionaryItem.getName().equals(identifier) || discretionaryItem.getDefinitionId().equals(identifier)) {
                return discretionaryItem;
            }
        }
        return null;
    }
}
