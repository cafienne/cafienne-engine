package org.cafienne.infrastructure.eventstore;

import akka.serialization.SerializerWithStringManifest;

import java.io.NotSerializableException;

public class OffsetSerializer extends SerializerWithStringManifest {
    @Override
    public int identifier() {
        return 6768;
    }

    @Override
    public String manifest(Object o) {
        return "Offset";
    }

    @Override
    public byte[] toBinary(Object o) {
        if (o instanceof WrappedOffset) {
            WrappedOffset wo = (WrappedOffset) o;
            return  (wo.offsetType().toString() + ":" + wo.offsetValue()).getBytes();
        }
        throw new RuntimeException("WrappedOffsetSerializer can only serialize WrappedOffset(s)");
    }

    @Override
    public Object fromBinary(byte[] bytes, String manifest) throws NotSerializableException, NotSerializableException {
        String bytesAsString = new String(bytes);
        return OffsetDeserializer.apply(bytesAsString);
    }
}
