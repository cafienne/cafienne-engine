package org.cafienne.actormodel.serialization.json;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;

import java.io.IOException;

public class DoubleValue extends NumericValue<Double> {
    public DoubleValue(double value) {
        super(value);
    }

    @Override
    public DoubleValue cloneValueNode() {
        return new DoubleValue(value);
    }
    
    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) {
        switch (propertyType) {
        case Double:
        case Float:
        case Decimal:
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