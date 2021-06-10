package org.cafienne.actormodel.serialization;

import org.cafienne.json.ValueMap;

/**
 * A {@link Migrator} can migrate a json AST (see {@link ValueMap}) to one newer version.
 * E.g., an event serialized in version 1 may be adjusted twice, resulting in a version 2 and 3.
 * Within a {@link Manifest#migrators()}, we can configure two {@link Migrator} classes; one to migrate content from
 * version 1 to version 2, and one to migrate version 2 content to version 3. Through this mechanism the 
 * deserializer can recover events from version 1, 2 and 3 back into the engine.
 * <p/>
 * <b>Note: each {@link Migrator} <code>MUST</code> be annotated with a {@link TargetVersion}.</b>
 */
@FunctionalInterface
public interface Migrator {
    /**
     * Migrate the original version into a newer version.
     * @param originalVersion
     * @return
     */
    ValueMap traverse(ValueMap originalVersion);
}
