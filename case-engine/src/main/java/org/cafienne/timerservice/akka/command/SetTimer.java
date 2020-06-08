/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.timerservice.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.timerservice.TimerService;
import org.cafienne.timerservice.akka.command.response.TimerServiceResponse;

import java.io.IOException;
import java.time.Instant;

/**
 *
 */
@Manifest
public class SetTimer extends TimerServiceCommand {
    public final String caseInstanceId;
    public final Instant moment;

    protected enum Fields {
        caseInstanceId, moment
    }

    /**
     * Ask timer service to ping the case task when the moment has come
     *
     */
    public SetTimer(TenantUser tenantUser, TimerEvent timer, Instant moment) {
        super(tenantUser, timer.getId());
        this.caseInstanceId = timer.getCaseInstance().getId();
        this.moment = moment;
    }

    public SetTimer(ValueMap json) {
        super(json);
        this.caseInstanceId = readField(json, Fields.caseInstanceId);
        this.moment = readInstant(json, Fields.moment);
    }

    /**
     */
    public TimerServiceResponse process(TimerService service) {
        return service.handle(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.caseInstanceId, caseInstanceId);
        writeField(generator, Fields.moment, moment);
    }
}
