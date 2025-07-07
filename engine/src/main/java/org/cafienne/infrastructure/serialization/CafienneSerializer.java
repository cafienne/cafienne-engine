/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.infrastructure.serialization;

import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.serialization.SerializerWithStringManifest;
import org.cafienne.infrastructure.serialization.serializers.CommandSerializers;
import org.cafienne.infrastructure.serialization.serializers.EventSerializers;
import org.cafienne.infrastructure.serialization.serializers.ResponseSerializers;
import org.cafienne.infrastructure.serialization.serializers.StorageSerializers;
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

    public static <T extends CafienneSerializable> T deserialize(ValueMap json) {
        String manifest = json.readString(Fields.manifest);
        return (T) new CafienneSerializer().fromJson(json.readMap(Fields.content), manifest);
    }

    static {
        EventSerializers.register();
        CommandSerializers.register();
        ResponseSerializers.register();
        StorageSerializers.register();
    }

    public static ManifestWrapper getManifest(String manifestString) {
        return manifests.get(manifestString);
    }

    public static <CS extends CafienneSerializable>void addManifestWrapper(Class<CS> eventClass, ValueMapDeserializer<CS> deserializer) {
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

    public CafienneSerializer() {
    }

    public CafienneSerializer(ExtendedActorSystem system) {
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

    /**
     * Deserialize an already parsed json structure into the object described by the manifest string
     * @param json
     * @param manifestString
     * @return
     */
    public Object fromJson(ValueMap json, String manifestString) {
        return deserialize(manifestString, () -> json, () -> json.toString().getBytes(StandardCharsets.UTF_8));
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
        throw new RuntimeException("CafienneSerializer can only serialize objects implementing CafienneSerializable");
    }
}

