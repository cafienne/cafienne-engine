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

package com.casefabric.infrastructure.serialization;

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
