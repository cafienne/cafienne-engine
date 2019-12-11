package org.cafienne.cmmn.repository;

/**
 * Simple exception class to be used when a DefinitionsDocument cannot be found
 */
public class MissingDefinitionException extends RuntimeException {
    public MissingDefinitionException(String msg) {
        super(msg);
    }

    public MissingDefinitionException(String msg, Throwable t) {
        super(msg, t);
    }
}
