package org.cafienne.akka.actor.serialization;

import org.cafienne.akka.actor.serialization.json.ValueMap;

/**
 * Deserializes a {@link ValueMap} based json AST into an object
 */
@FunctionalInterface
public interface ValueMapDeserializer<T extends CafienneSerializable> {
    T deserialize(ValueMap json);
}
