/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.response.migration;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponseWithValueMap;

/**
 * Response when a StartCase command is sent
 */
@Manifest
public class MigrationStartedResponse extends CaseResponseWithValueMap {
    public MigrationStartedResponse(CaseCommand command, ValueMap value) {
        super(command, value);
    }

    public MigrationStartedResponse(ValueMap json) {
        super(json);
    }
}
