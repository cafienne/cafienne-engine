/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.response.migration;

import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Response when a StartCase command is sent
 */
@Manifest
public class MigrationStartedResponse extends CaseResponse {
    public MigrationStartedResponse(CaseCommand command) {
        super(command);
    }

    public MigrationStartedResponse(ValueMap json) {
        super(json);
    }
}
