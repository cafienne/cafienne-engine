package org.cafienne.actormodel.serialization;

import org.cafienne.json.ValueMap;

/**
 * Deserializes a {@link ValueMap} based json AST into an object
 */
@FunctionalInterface
public interface ValueMapDeserializer<T extends CafienneSerializable> {
    T deserialize(ValueMap json);
}
