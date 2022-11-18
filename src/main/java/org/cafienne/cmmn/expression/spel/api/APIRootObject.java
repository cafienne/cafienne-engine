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

package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;

/**
 * Base context for SPEL expressions, enabling access to the case and it's public members from any expression.
 * <p>Some example expressions:
 * <ul>
 * <li><code>caseInstance.id</code> - The id of the case</li>
 * <li><code>user.id</code> - The unique id of the user executing the current command in the case</li>
 * <li><code>caseInstance.planItems.size()</code> - The number of plan items currently in the case</li>
 * <li><code>caseInstance.definition.name</code> - The name of case definition</li>
 * <li><code>caseInstance.definition.caseRoles</code> - The roles defined in the case</li>
 * </ul>
 * <p>
 * See {@link Case} itself for it's members.
 */
public abstract class APIRootObject<T extends ModelActor> extends APIObject<T> {
    /**
     * Option to pass a custom reader for the "user" property, e.g. a case team member instead of a Tenant user
     * @param model
     * @param user
     */
    protected APIRootObject(T model, APIObject<?> user) {
        super(model);
        addPropertyReader("user", () -> user);
    }

    protected APIRootObject(T model) {
        this(model, new UserContext(model, model.getCurrentUser()));
    }

    public ValueMap map(Object... args) {
        return this.Map(args);
    }

    public ValueMap Map(Object... args) {
        return new ValueMap(args);
    }

    public ValueList list(Object... args) {
        return this.List(args);
    }

    public ValueList List(Object... args) {
        return new ValueList(args);
    }

    public Object env(String key) {
        return System.getenv(key);
    }

    public abstract String getDescription();
}
