package org.cafienne.cmmn.instance.task.validation;

import org.cafienne.json.ValueMap;

public class ValidationResponse {
    private final ValueMap content;

    public ValidationResponse(ValueMap content) {
        this.content = content;
    }

    public boolean isValid() {
        return true;
    }

    public ValueMap getContent() {
        return content;
    }

    public Exception getException() {
        return null;
    }
}
