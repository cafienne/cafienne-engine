/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.platform.actorapi.response;

import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.platform.actorapi.command.PlatformCommand;

/**
 */
@Manifest
public class PlatformResponse extends ModelResponse {
    public PlatformResponse(PlatformCommand command) {
        super(command);
    }

    public PlatformResponse(ValueMap json) {
        super(json);
    }
}
