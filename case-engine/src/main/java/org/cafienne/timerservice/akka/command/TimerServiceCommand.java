/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.timerservice.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.CommandException;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.timerservice.TimerService;
import org.cafienne.timerservice.akka.command.response.TimerServiceResponse;

import java.io.IOException;

/**
 *
 */
public abstract class TimerServiceCommand extends ModelCommand<TimerService> implements BootstrapCommand {
    public final String timerId;

    /**
     * Create a new command that can be sent to the case.
     *
     */
    protected TimerServiceCommand(TenantUser tenantUser, String timerId) {
        super(tenantUser, TimerService.CAFIENNE_TIMER_SERVICE);
        this.timerId = timerId;
    }

    protected TimerServiceCommand(ValueMap json) {
        super(json);
        this.timerId = readField(json, Fields.timerId);
    }

    @Override
    public final Class<TimerService> actorClass() {
        return TimerService.class;
    }

    /**
     * Before the case starts processing the command, it will first ask to validate the command.
     * The default implementation is to check whether the case definition is available (i.e., whether StartCase command has been triggered before this command).
     * Implementations can override this method to implement their own validation logic.
     * Implementations may throw the {@link InvalidCommandException} if they encounter a validation error
     *
     * @param tenant
     * @throws InvalidCommandException If the command is invalid
     */
    public void validate(TimerService tenant) throws InvalidCommandException {
        // Nothing to validate, simply accept all commands
    }

    /**
     * Method invoked by the case in order to perform the actual command logic on the case.
     *
     * @param tenant
     * @return
     * @throws CommandException Implementations of this method may throw this exception if a failure happens while processing the command
     */
    public TimerServiceResponse process(TimerService tenant) {
        return new TimerServiceResponse(this);
    }

    @Override // Needed for bootstrap command
    public String tenant() {
        return "";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.timerId, timerId);
    }
}
