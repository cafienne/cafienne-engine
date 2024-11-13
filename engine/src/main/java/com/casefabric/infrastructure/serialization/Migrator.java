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

import com.casefabric.json.ValueMap;

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
