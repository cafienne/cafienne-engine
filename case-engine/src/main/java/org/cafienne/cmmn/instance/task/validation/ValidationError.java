package org.cafienne.cmmn.instance.task.validation;

import org.cafienne.cmmn.instance.casefile.ValueMap;

/**
 * Validation errors can occur if the TaskOutputValidator ran into some networking error and the like.
 */
public class ValidationError extends ValidationResponse {
    private final Exception exception;

    public ValidationError(String msg, Exception e) {
        super(new ValueMap("msg", msg, "error", e));
        this.exception = e;
    }

    public ValidationError(ValueMap msg, Exception e) {
        super(msg);
        msg.putRaw("error", e);
        this.exception = e;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public boolean isValid() {
        return false;
    }
}
