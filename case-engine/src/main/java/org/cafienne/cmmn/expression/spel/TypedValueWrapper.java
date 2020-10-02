package org.cafienne.cmmn.expression.spel;

import org.cafienne.akka.actor.serialization.json.Value;
import org.springframework.expression.TypedValue;

/**
 * Simple wrapper around value that extends SPEL's TypedValue, so that we can return the proper value from getValue()
 */
class TypedValueWrapper extends TypedValue {

    private final Value<?> value;

    TypedValueWrapper(Value<?> value) {
        super(value);
        this.value = value;
    }

    @Override
    public Object getValue() {
        if (value == null) {
            return null;
        } else if (value.isPrimitive()) {
            return value.getValue();
        } else {
            return value;
        }
    }

}
