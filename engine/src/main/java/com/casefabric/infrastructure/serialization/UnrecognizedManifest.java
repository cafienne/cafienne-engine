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

import java.io.Serializable;

/**
 * Wrapper for events with outdated or too new manifests (i.e. {@link CaseFabricSerializer} does not recognize them.
 */
public class UnrecognizedManifest implements Serializable {
    public final String manifest;
    public final byte[] blob;

    public UnrecognizedManifest(String manifest, byte[] blob) {
        this.manifest = manifest;
        this.blob = blob;
    }

    @Override
    public String toString() {
        return "Unrecognized manifest "+manifest+" with blob "+new String(blob);
    }
}
