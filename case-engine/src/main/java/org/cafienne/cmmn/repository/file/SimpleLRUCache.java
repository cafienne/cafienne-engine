package org.cafienne.cmmn.repository.file;

import com.typesafe.config.Config;
import org.cafienne.akka.actor.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Too simple SimpleLRUCache, that takes it's max size from a Config object, defaulting to 100.
 */
public class SimpleLRUCache<K, T> extends LinkedHashMap<K, T> {
    private final static Logger logger = LoggerFactory.getLogger(FileBasedDefinitionProvider.class);
    private int maxSize = 100;

    public SimpleLRUCache() {
        super(16, 0.75f, true);
        Config config = CaseSystem.config().repository().config();
        if (config.hasPath("cache.size")) {
            maxSize = config.getInt("cache.size");
        }
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, T> eldest) {
        boolean remove = size() > maxSize;
        if (remove) {
            // Printing that we remove from the cache, just to inform that the cache size may not be large enough.
            logger.debug("Removing definitions document " + eldest.getKey() + " from the cache, since max cache size " + maxSize + " has been reached.");
        }
        return remove;
    }
}
