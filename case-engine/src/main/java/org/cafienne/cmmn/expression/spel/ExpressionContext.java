package org.cafienne.cmmn.expression.spel;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.Case;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
abstract class ExpressionContext implements SpelReadable {
    private final ModelActor model;
    /**
     * Set of accessible property names. Case sensitive, and does not contain deprecated properties
     */
    private final Set<String> propertyNames = new HashSet();
    private final Map<String, ExpressionContextPropertyReader> readers = new HashMap();

    // Perhaps extend later with other information, such as CaseTeam or current user?
    // but for now, caseInstance is sufficient as a starting path to fetch any required information

    protected ExpressionContext(ModelActor model) {
        this.model = model;
        UserWrapper user = new UserWrapper(model.getCurrentUser());
        addPropertyReader("case", () -> model);
        addPropertyReader("user", () -> user);
        addDeprecatedReader("caseInstance", "case", () -> model);
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

    protected void addPropertyReader(String propertyName, ExpressionContextPropertyReader reader) {
        if (propertyName != null) {
            propertyNames.add(propertyName); // The case-sensitive version
            readers.put(propertyName.toLowerCase(), reader);
        }
    }

    protected void addDeprecatedReader(String deprecatedName, String newPropertyName, ExpressionContextPropertyReader reader) {
        readers.put(deprecatedName.toLowerCase(), () -> {
            model.addDebugInfo(() -> "Expression contains deprecated property '" + deprecatedName + "'; please use property '" + newPropertyName + "' instead");
            return reader.get();
        });
    }

    @Override
    public boolean canRead(String propertyName) {
        boolean found = readers.keySet().contains(propertyName.toLowerCase());
        if (!found) {
            model.addDebugInfo(() -> "Property " + propertyName + " is not available on this " + getClass().getSimpleName() + "; available properties: " + propertyNames);
        }
        return found;
    }

    @Override
    public Value<?> read(String propertyName) {
        Object value = readers.getOrDefault(propertyName.toLowerCase(), () -> null).get();
        if (value == null) {
            return null;
        } else {
            return Value.convert(value);
        }
    }
}

