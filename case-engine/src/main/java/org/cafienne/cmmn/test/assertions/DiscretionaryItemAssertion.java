package org.cafienne.cmmn.test.assertions;

import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.cmmn.test.CaseTestCommand;

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
    public void assertType(String expectedType) {
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

    public String getType() {
        return item.raw("type");
    }

    public String getParentId() {
        return item.raw("parentId");
    }

    @Override
    public String toString() {
        return item.toString();
    }
}
