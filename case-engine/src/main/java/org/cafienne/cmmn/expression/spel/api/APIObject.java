package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.cmmn.expression.spel.SpelReadable;
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
public abstract class APIObject<T extends ModelActor> implements SpelReadable {
    /**
     * Set of accessible property names. Case sensitive, and does not contain deprecated properties
     */
    private final Set<String> propertyNames = new HashSet();
    private final Map<String, ExpressionObjectPropertyReader> readers = new HashMap();
    protected final T actor;

    protected APIObject(T actor) {
        this.actor = actor;
        UserContext user = new UserContext(actor.getCurrentUser());
        addPropertyReader("user", () -> user);
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

    protected void addContextProperty(APIObject context, String propertyName, String deprecatedName) {
        addPropertyReader(propertyName, () -> context);
        addDeprecatedReader(deprecatedName, propertyName, () -> context);
    }

    protected void addPropertyReader(String propertyName, ExpressionObjectPropertyReader reader) {
        if (propertyName != null) {
            addReader(propertyName, reader);
        }
    }

    protected void warnDeprecation(String deprecatedName, String newPropertyName) {
        getActor().addDebugInfo(() -> "Expression contains deprecated property '" + deprecatedName + "'; please use property '" + newPropertyName + "' instead");
    }

    protected void addDeprecatedReader(String deprecatedName, String newPropertyName, ExpressionObjectPropertyReader reader) {
        System.out.println("REGISTER DEPRECATED READER " + deprecatedName +" of type " + reader);

        addReader(deprecatedName, () -> {
            warnDeprecation(deprecatedName, newPropertyName);
            return reader.get();
        });
    }

    @Override
    public boolean canRead(String propertyName) {
        System.out.println("Checking " + propertyName +" on " + this.getClass().getSimpleName() +" with props : " + propertyNames);
        boolean found = readers.keySet().contains(propertyName.toLowerCase());
        if (!found) {
            getActor().addDebugInfo(() -> "Property " + propertyName + " is not available on the " + getClass().getSimpleName() + "; available properties: " + propertyNames);
        }
        return found;
    }

    @Override
    public Object read(String propertyName) {
        return readers.getOrDefault(propertyName.toLowerCase(), () -> null).get();
    }
}

