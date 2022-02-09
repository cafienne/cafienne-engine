package org.cafienne.cmmn.definition;

public interface DefinitionElement {
    String getId();

    String getName();

    String getType();

    boolean differs(Object object);
}
