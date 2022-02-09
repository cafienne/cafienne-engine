package org.cafienne.cmmn.repository.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Too simple SimpleLRUCache, that takes it's max size from a Config object, defaulting to 100.
 */
public class SimpleLRUCache<K, T> extends LinkedHashMap<K, T> {
    private final static Logger logger = LoggerFactory.getLogger(SimpleLRUCache.class);
    private final int maxSize;

    public SimpleLRUCache(int maxSize) {
        super(maxSize >= 0 ? maxSize : 0, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    public T put(K key, T value) {
        // Overriding this to avoid unnecessary removing eldest entry invocations
        if (maxSize >= 0) {
            return super.put(key, value);
        } else {
            return null;
        }
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, T> eldest) {
        boolean remove = size() > maxSize;
        if (remove) {
            // Printing that we remove from the cache, just to inform that the cache size may not be large enough.
            logger.debug("Removing " + eldest.getClass().getSimpleName() + " " + eldest.getKey() + " from the cache, since max cache size " + maxSize + " has been reached.");
        }
        return remove;
    }
}
