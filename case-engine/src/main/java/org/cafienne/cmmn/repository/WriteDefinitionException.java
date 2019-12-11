package org.cafienne.cmmn.repository;

/**
 * Simple exception thrown if it is not possible to write a DefinitionsDocument.
 */
public class WriteDefinitionException extends Exception {
    public WriteDefinitionException(String msg, Throwable t) {
        super(msg, t);
    }
}
