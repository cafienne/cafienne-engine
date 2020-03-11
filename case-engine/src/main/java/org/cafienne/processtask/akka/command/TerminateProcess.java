/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.akka.command.response.ProcessResponse;
import org.cafienne.processtask.akka.event.ProcessTerminated;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class TerminateProcess extends ProcessCommand {
    public TerminateProcess(TenantUser tenantUser, String id) {
        super(tenantUser, id);
    }

    public TerminateProcess(ValueMap json) {
        super(json);
    }

    @Override
    public ProcessResponse process(ProcessTaskActor process) {
        process.addEvent(new ProcessTerminated(process));
        return new ProcessResponse(this);
    }
}
