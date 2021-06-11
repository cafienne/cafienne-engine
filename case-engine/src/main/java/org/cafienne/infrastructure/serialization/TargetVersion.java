package org.cafienne.infrastructure.serialization;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Target version must be set on a {@link Migrator}. It determines to which version
 * the migration path can be traversed. The value must be a positive number.
 * E.g., {@link TargetVersion(3)} on a {@link Migrator} will migrate json ASTs in version 2 to version 3.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TargetVersion {
    int value();
}
