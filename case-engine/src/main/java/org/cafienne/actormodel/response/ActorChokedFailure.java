/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.actormodel.response;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Can be used to return an exception to the sender of the command when the engine ran into some non-functional exception,
 * e.g. during handling of a command.
 */
@Manifest
public class ActorChokedFailure extends CommandFailure {
    /**
     * Create a failure response for the command.
     * The message id of the command will be pasted into the message id of the response.
     *
     * @param command
     * @param failure The reason why the command failed
     */
    public ActorChokedFailure(ModelCommand command, Throwable failure) {
        super(command, failure);
    }

    public ActorChokedFailure(ValueMap json) {
        super(json);
    }
}
