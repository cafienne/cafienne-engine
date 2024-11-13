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

package com.casefabric.actormodel.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.ModelActor;
import com.casefabric.infrastructure.CaseFabricVersion;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

@Manifest
public class EngineVersionChanged extends BaseModelEvent<ModelActor> {

    private final CaseFabricVersion version;

    public EngineVersionChanged(ModelActor actor, CaseFabricVersion version) {
        super(actor);
        this.version = version;
    }

    public EngineVersionChanged(ValueMap json) {
        super(json);
        this.version = json.readObject(Fields.version, CaseFabricVersion::new);
    }

    @Override
    public void updateState(ModelActor actor) {
        actor.setEngineVersion(this.version);
    }

    /**
     * Returns the version of the engine that is currently applied in the case
     * @return
     */
    public CaseFabricVersion version() {
        return version;
    }

    @Override
    public String toString() {
        return "Engine version changed to " + version;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        super.writeField(generator, Fields.version, version.json());
    }
}
