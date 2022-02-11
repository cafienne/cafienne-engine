package org.cafienne.cmmn.expression.spel.api;

/**
 * Simple functional interface to return a value upon invocation of the getter
 */
@FunctionalInterface
public interface ExpressionObjectPropertyReader {
    /**
     * Get the value of the property
     * @return
     */
    Object get();
}