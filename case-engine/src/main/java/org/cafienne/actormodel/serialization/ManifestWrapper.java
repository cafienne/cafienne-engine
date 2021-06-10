package org.cafienne.actormodel.serialization;

import org.cafienne.json.ValueMap;

import java.lang.reflect.InvocationTargetException;

/**
 * Manifest wrapper is simple wrapper class around the {@link Manifest}, that parses and analyses
 * it in order to have faster usage in runtime.
 */
public class ManifestWrapper {
    /**
     * The string used to separate the version number from the manifest
     */
    private static final String VERSION_SEPARATOR = ";";

    /**
     * Actual manifest that is wrapped in this class. The manifest is used to find out information
     * on the actual manifest string, as well as information about the current version of the manifest
     * and an optional list of migrators that can be used to deserialize older versions of the manifest.
     */
    public final Manifest manifest;
    /**
     * The actual class that the manifest can be serialized and deserialized with the manifest
     */
    public final Class<?> eventClass;
    /**
     * The deserializer that goes with the manifest.
     */
    public final ValueMapDeserializer<?> deserializer;
    /**
     * Raw manifest string (e.g. <code>"CaseDefinitionApplied"</code>) <i>without</i> version information.
     */
    public final String string;
    /**
     * Current manifest version
     */
    public final int version;
    /**
     * Manifest string with current version number included.
     */
    public final String current;
    /**
     * Ordered list of {@link Migrator} for this manifest to resolve from version 0 to current version
     */
    final Migrator[] migrators;
    /**
     * The list of manifest versions supported by this Manifest.
     */
    public final String[] manifestsByVersion;

    public ManifestWrapper(Class<?> eventClass, ValueMapDeserializer<?> deserializer) {
        if (eventClass == null) {
            throw new NullPointerException("Cannot create a ManifestWrapper without an event class");
        }
        if (deserializer == null) {
            throw new NullPointerException("The ManifestWrapper for " + eventClass.getName() + " must provide a ValueMapDeserializer");
        }
        this.eventClass = eventClass;
        this.deserializer = deserializer;
        this.manifest = eventClass.getAnnotation(Manifest.class);
        if (manifest == null) {
            throw new RuntimeException("The ManifestWrapper for " + eventClass.getName() + " must have an annotation of type "+Manifest.class.getName());
        }
        this.string = manifest.value().isEmpty() ? eventClass.getSimpleName() : manifest.value();
        this.version = manifest.version();
        this.current = version == 0 ? string : string + VERSION_SEPARATOR + version;
        this.migrators = new Migrator[manifest.version()];
        this.manifestsByVersion = new String[manifest.version() + 1];
        analyzeMigrators();
        determineManifestStrings();
    }

    private void analyzeMigrators() {
        // The migrators may not be declared in a specific order. However,
        //  we want them in proper order. First from version 0 to version 1, then from v1 to v2, etc.
        // So here we instantiate and order them. Having them instantiated means we only
        //  need to do this once per manifest, instead of upon each event migration.
        Class<? extends Migrator>[] possiblyUnorderedMigrators = manifest.migrators();
        for (int i = 0; i < possiblyUnorderedMigrators.length; i++) {
            Class<? extends Migrator> migratorClass = possiblyUnorderedMigrators[i];
            try {
                Migrator migrator = migratorClass.getDeclaredConstructor().newInstance();
                TargetVersion target = migrator.getClass().getAnnotation(TargetVersion.class);
                if (target == null) {
                    throw new RuntimeException("Migrator " + migratorClass.getName() + " misses a TargetVersion annotation");
                }
                if (target.value() <= 0) {
                    throw new RuntimeException("Migrator " + migratorClass.getName() + " has an invalid TargetVersion (" + target.value() + "). TargetVersions must be 1 or higher.");
                }
                int fromVersion = target.value() - 1;
                // TODO: Must we handle those cases where the fromVersion is higher than current version or lower than 0?
                // And what about Migrators that have no TargetVersion? Can we somehow enforce this through code?
                migrators[fromVersion] = migrator;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException("Migrator class " + migratorClass.getName() + " in ManifestWrapper of " + eventClass.getName() + " cannot be instantiated", e.fillInStackTrace());
            }
        }
    }

    private void determineManifestStrings() {
        for (int i = version; i > 0; i--) {
            manifestsByVersion[i] = string + VERSION_SEPARATOR + i;
        }
        manifestsByVersion[0] = string; // Version 0 is not post-fixed with 0; but goes as a raw manifest.
    }

    /**
     * Migrates an event AST from an older version to the expected version.
     *
     * @param original
     * @param olderVersionManifest
     * @return
     */
    public ValueMap migrate(ValueMap original, String olderVersionManifest) {
        if (current.equals(olderVersionManifest)) {
            // no need to migrate, it is already in expected version;
            return original;
        }

        int olderVersion = 0;
        int semicolonPosition = olderVersionManifest.lastIndexOf(VERSION_SEPARATOR);
        if (semicolonPosition > 0) {
            // TODO: must we handle those cases where the prefix to the first semicolon is not a number?
            olderVersion = Integer.parseInt(olderVersionManifest.substring(semicolonPosition + 1));
        }
        for (int i = olderVersion; i < version; i++) {
            original = migrators[i].traverse(original);
        }
        return original;
    }

    @Override
    public String toString() {
        // Returns the manifest representation.
        //  For easy debugging of migrations, you can temporarily change this to "string" instead of "current",
        //  as during serialization it will store the events as if they are version 0, and if there is a manifest with
        //  a newer version then upon deserialization it will go through the migrators.
        return current;
    }
}
