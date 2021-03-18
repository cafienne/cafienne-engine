package org.cafienne.cmmn.expression.spel;

/**
 * Simple functional interface to return a value upon invocation of the getter
 */
@FunctionalInterface
public interface ExpressionContextPropertyReader {
    /**
     * Get the value of the property
     * @return
     */
    Object get();
}