package org.cafienne.cmmn.test;

import akka.serialization.JSerializer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TestSerializer extends JSerializer {

    private Map<String, Object> cache = new LinkedHashMap<>();

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        String id = new String(bytes);
        if (cache.containsKey(id)) {
            Object o = cache.get(id);
            cache.remove(id, o);
            return o;
        }

        return null;
    }

    @Override
    public int identifier() {
        return 999;
    }

    @Override
    public byte[] toBinary(Object o) {
        String id = UUID.randomUUID().toString();
        cache.put(id, o);
        return id.getBytes();
    }

    @Override
    public boolean includeManifest() {
        return false;
    }
}
