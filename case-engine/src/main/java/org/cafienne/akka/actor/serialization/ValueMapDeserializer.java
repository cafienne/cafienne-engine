package org.cafienne.akka.actor.serialization;

import org.cafienne.cmmn.instance.casefile.ValueMap;

/**
 * Deserializes a {@link ValueMap} based json AST into an object
 */
@FunctionalInterface
public interface ValueMapDeserializer<T extends AkkaSerializable> {
    T deserialize(ValueMap json);
}
