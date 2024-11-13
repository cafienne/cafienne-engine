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

package com.casefabric.tenant.actorapi.event.user;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.identity.TenantUser;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;
import com.casefabric.tenant.TenantActor;
import com.casefabric.tenant.actorapi.event.TenantBaseEvent;

import java.io.IOException;

public abstract class TenantMemberEvent extends TenantBaseEvent {
    private final TenantUser user;
    public final TenantUser member;
    public final String memberId;

    protected TenantMemberEvent(TenantActor tenant, TenantUser user) {
        super(tenant);
        this.user = user;
        this.member = this.user;
        this.memberId = this.user.id();
    }

    protected TenantMemberEvent(ValueMap json) {
        super(json);
        this.user = json.readObject(Fields.user, TenantUser::deserialize);
        this.member = this.user;
        this.memberId = this.user.id();
    }

    protected void writeTenantUserEvent(JsonGenerator generator) throws IOException {
        super.writeTenantEvent(generator);
        writeField(generator, Fields.user, user);
    }
}
