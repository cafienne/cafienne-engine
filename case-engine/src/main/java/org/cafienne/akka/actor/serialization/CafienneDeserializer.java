package org.cafienne.akka.actor.serialization;

import org.cafienne.akka.actor.serialization.json.Value;

@FunctionalInterface
public interface CafienneDeserializer<T extends CafienneSerializable> {
    T deserialize(Value<?> json);
}
