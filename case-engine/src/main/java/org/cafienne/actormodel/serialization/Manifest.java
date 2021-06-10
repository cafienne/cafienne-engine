package org.cafienne.actormodel.serialization;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A Manifest contains the manifest information that will be used in the akka serialization of
 * case engine events. The manifest consists of a string and an optional version number.
 * If the {@link CafienneSerializer} encounters a manifest of an older version, it will check
 * whether there are {@link Migrator} classes defined along with the {@link Manifest} and use that to migrate
 * the json ast before deserializing the event.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Manifest {
    /**
     * By default the value ought to return the content of the manifest string <i>without</i> the version.
     * If the value is empty, then the simple class name will be used to determine the manifest string.
     * @return
     */
    String value() default "";

    /**
     * Current version of this {@link Manifest}. The default version is 0.
     * The version of the manifest is prefixed to the manifest string that is used in akka serialization by
     * the {@link CafienneSerializer}.
     * @return
     */
    int version() default 0;

    /**
     * List of {@link Migrator} that can be used to migrate older versions of the manifest to the current {@link #version}.
     * The migrators need not be ordered, as long as there are no gaps in the various migrators after they have been 
     * ordered by the setup mechanism. I.e., each {@link Migrator} must have a {@link TargetVersion},
     * and the set of all {@link TargetVersion} objects must be 0, 1, 2, ... until the current version in the {@link Manifest}.
     * @return
     */
    Class<? extends Migrator>[] migrators() default {};
}
