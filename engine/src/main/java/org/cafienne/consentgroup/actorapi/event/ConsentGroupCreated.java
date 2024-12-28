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

package org.cafienne.consentgroup.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ConsentGroupCreated extends ConsentGroupBaseEvent implements BootstrapMessage {
    public final CafienneVersion engineVersion;
    public final String tenant;

    public ConsentGroupCreated(ConsentGroupActor group, String tenant) {
        super(group);
        this.engineVersion = Cafienne.version();
        this.tenant = tenant;
    }

    public ConsentGroupCreated(ValueMap json) {
        super(json);
        this.tenant = json.readString(Fields.tenant);
        this.engineVersion = json.readObject(Fields.engineVersion, CafienneVersion::new);
    }

    @Override
    public void updateState(ConsentGroupActor group) {
        group.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeConsentGroupEvent(generator);
        writeField(generator, Fields.tenant, this.tenant);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}
