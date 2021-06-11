package org.cafienne.json;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;

import java.io.IOException;

public class LongValue extends NumericValue<Long> {
    public LongValue(long value) {
        super(value);
    }
    
    @Override
    public LongValue cloneValueNode() {
        return new LongValue(value);
    }
    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        switch (propertyType) {
        case Integer:
        case String: // Hmmm, do we really match strings?
        case Unspecified:
            return true;
        default:
            return baseMatch(propertyType);
        }
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeNumber(value);
    }
}