/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.actorapi.command;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
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
        return process.terminate(this);
    }
}
