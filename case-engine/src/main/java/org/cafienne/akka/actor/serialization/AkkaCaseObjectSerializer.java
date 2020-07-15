package org.cafienne.akka.actor.serialization;

import akka.actor.ExtendedActorSystem;
import akka.serialization.SerializerWithStringManifest;
import org.cafienne.akka.actor.serialization.json.JSONParseFailure;
import org.cafienne.akka.actor.serialization.json.JSONReader;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AkkaCaseObjectSerializer extends SerializerWithStringManifest {
    private final static Logger logger = LoggerFactory.getLogger(AkkaCaseObjectSerializer.class);

    private final static Map<String, ManifestWrapper> manifests = new HashMap();
    private final static Map<Class<?>, ManifestWrapper> manifestsByClass = new HashMap();

    static ManifestWrapper getManifest(String manifestString) {
        return manifests.get(manifestString);
    }

    static void addManifestWrapper(Class<?> eventClass, ValueMapDeserializer<?> deserializer) {
        ManifestWrapper manifest = new ManifestWrapper(eventClass, deserializer);
        manifestsByClass.put(manifest.eventClass, manifest);
        // Now register manifest strings of all versions, starting from the current
        for (String manifestString : manifest.manifestsByVersion) {
            manifests.put(manifestString, manifest);
        }
    }

    protected AkkaCaseObjectSerializer() {
    }

    protected AkkaCaseObjectSerializer(ExtendedActorSystem system) {
    }

    @Override
    public Object fromBinary(byte[] eventBlob, String manifestString) {
        try {
            // Get the right manifest
            ManifestWrapper manifest = getManifest(manifestString);
            if (manifest == null) {
                // This is an unrecognized event.
                logger.warn("Manifest " + manifestString +" cannot be converted to one of the registered event types. Generating 'UnrecognizedManifest' object instead");
                return new UnrecognizedManifest(manifestString, eventBlob);
            }
            try {
                ValueMap value = JSONReader.parse(eventBlob);
                // Run migrators on the json AST
                value = manifest.migrate(value, manifestString);
                // Invoke the deserializer
                ValueMapDeserializer<?> deserializer = manifest.deserializer;
                return deserializer.deserialize(value);
            } catch (IOException | JSONParseFailure | DeserializationError e) {
                return new DeserializationFailure(manifestString, e, eventBlob);
            }
        } catch (Throwable t) {
            logger.error("Deserialization failure (manifest " + manifestString + ")", t);
            throw t;
        }
    }

    @Override
    public String manifest(Object o) {
        if (o instanceof AkkaSerializable) {
            ManifestWrapper manifest = manifestsByClass.get(o.getClass());
            if (manifest != null) {
                return manifest.toString();
            } else {
                throw new RuntimeException("A manifest wrapper for class "+o.getClass().getName()+" has not been registered");
            }
        }
        throw new RuntimeException("The Akka Case Object Serializer can only serialize objects implementing AkkaSerializable");
    }

    @Override
    public byte[] toBinary(Object o) {
        if (o instanceof AkkaSerializable) {
            AkkaSerializable target = (AkkaSerializable) o;
            return target.toBytes();
        }
        throw new RuntimeException("The Akka Case Object Serializer can only serialize objects implementing AkkaSerializable");
    }

}

