package org.cafienne.cmmn.expression.spel.api.cmmn.file;

import org.cafienne.json.Value;
import org.cafienne.cmmn.expression.spel.SpelPropertyValueProvider;
import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;

/**
 */
public class ValueAPI extends APIObject<Case> implements SpelPropertyValueProvider {
    private final Value value;

    public ValueAPI(CaseFileItem item) {
        super(item.getCaseInstance());
        this.value = item.getValue();
    }

    @Override
    public Object getValue() {
        if (value.isPrimitive()) {
            Object primitiveValue = value.getValue();
            return primitiveValue;
        } else {
            return value;
        }
    }
}
