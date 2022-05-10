package org.cafienne.infrastructure.serialization;

import akka.actor.ExtendedActorSystem;
import akka.serialization.SerializerWithStringManifest;
import org.cafienne.infrastructure.serialization.serializers.CommandSerializers;
import org.cafienne.infrastructure.serialization.serializers.EventSerializers;
import org.cafienne.infrastructure.serialization.serializers.ResponseSerializers;
import org.cafienne.json.JSONParseFailure;
import org.cafienne.json.JSONReader;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CafienneSerializer extends SerializerWithStringManifest {
    private final static Logger logger = LoggerFactory.getLogger(CafienneSerializer.class);

    /**
     * The unique identifier for the CafienneSerializer (value is <code>424242</code>)
     */
    public static final int IDENTIFIER = 52943;

    private final static Map<String, ManifestWrapper> manifests = new HashMap<>();
    private final static Map<Class<?>, ManifestWrapper> manifestsByClass = new HashMap<>();

    static {
        EventSerializers.register();
        CommandSerializers.register();
        ResponseSerializers.register();
    }

    static ManifestWrapper getManifest(String manifestString) {
        return manifests.get(manifestString);
    }

    public static void addManifestWrapper(Class<?> eventClass, ValueMapDeserializer<?> deserializer) {
        ManifestWrapper manifest = new ManifestWrapper(eventClass, deserializer);
        manifestsByClass.put(manifest.eventClass, manifest);
        // Now register manifest strings of all versions, starting from the current
        for (String manifestString : manifest.manifestsByVersion) {
            manifests.put(manifestString, manifest);
        }
    }

    public static String getManifestString(Object o) {
        ManifestWrapper manifest = manifestsByClass.get(o.getClass());
        if (manifest != null) {
            return manifest.toString();
        } else {
            if (o instanceof CafienneSerializable) {
                throw new RuntimeException("A manifest wrapper for class " + o.getClass().getName() + " has not been registered");
            } else {
                throw new RuntimeException("CafienneSerializer can only serialize objects implementing CafienneSerializable");
            }
        }
    }

    protected CafienneSerializer() {
    }

    protected CafienneSerializer(ExtendedActorSystem system) {
    }

    @FunctionalInterface // Simplistic interface to avoid an if statement in the deserialize function
    interface ValueMapProvider {
        ValueMap giveJson() throws IOException, JSONParseFailure;
    }

    @FunctionalInterface // Simplistic interface to avoid an if statement in the deserialize function
    interface EventBlobProvider {
        byte[] giveMeTheBytes();
    }

    private Object deserialize(String manifestString, ValueMapProvider jsonProvider, EventBlobProvider bytesProvider) {
        // Get the right manifest
        ManifestWrapper manifest = getManifest(manifestString);
        if (manifest == null) {
            // This is an unrecognized event.
            logger.warn("Manifest " + manifestString + " cannot be converted to one of the registered event types. Generating 'UnrecognizedManifest' object instead");
            return new UnrecognizedManifest(manifestString, bytesProvider.giveMeTheBytes());
        }
        try {
            // Run migrators on the json AST
            ValueMap value = jsonProvider.giveJson();
            value = manifest.migrate(value, manifestString);
            // Invoke the deserializer
            ValueMapDeserializer<?> deserializer = manifest.deserializer;
            return deserializer.deserialize(value);
        } catch (DeserializationError e) {
            return new DeserializationFailure(manifestString, e, bytesProvider.giveMeTheBytes());
        } catch (Exception e) {
            return new DeserializationFailure(manifestString, e, bytesProvider.giveMeTheBytes());
        }
    }

    @Override
    public Object fromBinary(byte[] eventBlob, String manifestString) {
        return deserialize(manifestString, () -> JSONReader.parse(eventBlob), () -> eventBlob);
    }

    @Override
    public int identifier() {
        return IDENTIFIER;
    }

    @Override
    public String manifest(Object o) {
        return getManifestString(o);
    }

    @Override
    public byte[] toBinary(Object o) {
        if (o instanceof CafienneSerializable) {
            CafienneSerializable target = (CafienneSerializable) o;
            return target.toBytes();
        }
        throw new RuntimeException("The Akka Case Object Serializer can only serialize objects implementing CafienneSerializable");
    }
}

