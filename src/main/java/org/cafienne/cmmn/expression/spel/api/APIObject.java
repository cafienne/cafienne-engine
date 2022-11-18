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
import org.cafienne.cmmn.expression.spel.SpelReadable;
import org.cafienne.json.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base context for SPEL expressions, enabling access to the case and it's public members from any expression.
 * <p>Some example expressions:
 * <ul>
 * <li><code>case.id</code> - The id of the case</li>
 * <li><code>user.id</code> - The unique id of the user executing the current command in the case</li>
 * <li><code>case.plan.MyTaskName</code> - A reference to the task with the name 'MyTaskName' in the case plan</li>
 * <li><code>case.name</code> - The name of the case definition</li>
 * </ul>
 * <p>
 */
public abstract class APIObject<T extends ModelActor> implements SpelReadable {
    private final static Logger logger = LoggerFactory.getLogger(APIObject.class);

    /**
     * Set of accessible property names. Case sensitive, and does not contain deprecated properties
     */
    private final Set<String> propertyNames = new HashSet<>();
    private final Set<String> deprecatedNames = new HashSet<>();
    private final Map<String, ExpressionObjectPropertyReader> readers = new HashMap<>();
    protected final T actor;

    protected APIObject(T actor) {
        this.actor = actor;
    }

    public T getActor() {
        return actor;
    }

    private void addReader(String propertyName, ExpressionObjectPropertyReader reader) {
        propertyNames.add(propertyName); // The case-sensitive version, used to print in log messages if a property cannot be found.
        readers.put(propertyName.toLowerCase(), reader);
    }

    protected void addProperty(String propertyName, Object property) {
        addPropertyReader(propertyName, () -> property);
    }

    protected void addContextProperty(APIObject<T> context, String propertyName, String deprecatedName) {
        addPropertyReader(propertyName, () -> context);
        addDeprecatedReader(deprecatedName, propertyName, () -> context);
    }

    protected void addPropertyReader(String propertyName, ExpressionObjectPropertyReader reader) {
        if (propertyName != null) {
            addReader(propertyName, reader);
        }
    }

    protected void warnDeprecation(String deprecatedName, String newPropertyName) {
        String msg = "Expression contains deprecated property '" + deprecatedName + "'; please use property '" + newPropertyName + "' instead";
        logger.warn(msg);
        getActor().addDebugInfo(() -> msg);
    }

    protected void addDeprecatedReader(String deprecatedName, String newPropertyName, ExpressionObjectPropertyReader reader) {
        deprecatedNames.add(deprecatedName);
        addReader(deprecatedName, () -> {
            warnDeprecation(deprecatedName, newPropertyName);
            return reader.get();
        });
    }

    protected void addDeprecatedReader(String deprecatedName, ExpressionObjectPropertyReader reader) {
        deprecatedNames.add(deprecatedName);
        addReader(deprecatedName, () -> {
            String msg = "Expression contains unsupported property '" + deprecatedName + "'. An empty value is given.";
            logger.warn(msg);
            getActor().addDebugInfo(() -> msg);
            return reader.get();
        });
    }

    @Override
    public boolean canRead(String propertyName) {
        if (!readers.containsKey(propertyName.toLowerCase())) {
            Set<String> availableProperties = new HashSet<>(propertyNames);
            availableProperties.removeAll(deprecatedNames);
            getActor().addDebugInfo(() -> "Reading property '" + propertyName + "' has no value in " + getClass().getSimpleName() + ". Values exist for: " + availableProperties);
        }
        return true;
    }

    @Override
    public Object read(String propertyName) {
        // Returning Value.NULL makes it possible to read further into the non-existing property,
        //  avoiding spel expressions to crash or not to have to use "safe?" expressions.
        return readers.getOrDefault(propertyName.toLowerCase(), () -> Value.NULL).get();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}

