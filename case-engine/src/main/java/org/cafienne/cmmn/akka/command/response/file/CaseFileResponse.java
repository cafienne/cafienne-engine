/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.response.file;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.CaseFileCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;

/**
 *
 */
@Manifest
public class CaseFileResponse extends CaseResponse {
   public CaseFileResponse(CaseFileCommand command) {
        super(command);
    }

    public CaseFileResponse(ValueMap json) {
        super(json);
    }

    @Override
    public String toString() {
        return "CaseFileResponse for "+getActorId()+": last modified is "+getLastModified();
    }
}
