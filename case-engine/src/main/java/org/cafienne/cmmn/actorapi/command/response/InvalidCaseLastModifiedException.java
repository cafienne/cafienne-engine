package org.cafienne.cmmn.actorapi.command.response;


/**
 * Thrown when invalid CaseLastModified content is present in the request header
 */
public class InvalidCaseLastModifiedException extends Exception {
    public InvalidCaseLastModifiedException(String msg) {
        super(msg);
    }
}