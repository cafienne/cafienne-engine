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

package com.casefabric.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.event.CommitEvent;
import com.casefabric.cmmn.actorapi.command.platform.PlatformUpdate;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

@Manifest
public class CaseAppliedPlatformUpdate extends CaseBaseEvent implements CommitEvent {
    public final PlatformUpdate newUserInformation;

    public CaseAppliedPlatformUpdate(Case tenant, PlatformUpdate newUserInformation) {
        super(tenant);
        this.newUserInformation = newUserInformation;
    }

    public CaseAppliedPlatformUpdate(ValueMap json) {
        super(json);
        newUserInformation = PlatformUpdate.deserialize(json.withArray(Fields.users));
    }

    @Override
    public void updateState(Case actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.users, newUserInformation.toValue());
    }
}
