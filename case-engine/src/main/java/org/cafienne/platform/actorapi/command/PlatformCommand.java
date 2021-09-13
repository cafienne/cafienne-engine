/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.platform.actorapi.command;

import org.cafienne.actormodel.command.BootstrapCommand;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.json.ValueMap;
import org.cafienne.platform.PlatformService;
import org.cafienne.platform.actorapi.response.PlatformResponse;

/**
 *
 */
public abstract class PlatformCommand extends ModelCommand<PlatformService> implements BootstrapCommand {
    /**
     * Create a new command that can be sent to the case.
     *
     */
    protected PlatformCommand(TenantUser tenantUser) {
        super(tenantUser, PlatformService.CAFIENNE_PLATFORM_SERVICE);
    }

    protected PlatformCommand(ValueMap json) {
        super(json);
    }

    @Override
    public final Class<PlatformService> actorClass() {
        return PlatformService.class;
    }

    /**
     * Before the case starts processing the command, it will first ask to validate the command.
     * The default implementation is to check whether the case definition is available (i.e., whether StartCase command has been triggered before this command).
     * Implementations can override this method to implement their own validation logic.
     * Implementations may throw the {@link InvalidCommandException} if they encounter a validation error
     *
     * @param platform
     * @throws InvalidCommandException If the command is invalid
     */
    public void validate(PlatformService platform) throws InvalidCommandException {
        if (! Cafienne.isPlatformOwner(getUser())) {
            throw new AuthorizationException("Only platform owners can invoke platform commands");
        }
    }

    /**
     * Method invoked by the case in order to perform the actual command logic on the case.
     *
     * @param platform
     * @return
     * @throws CommandException Implementations of this method may throw this exception if a failure happens while processing the command
     */
    public PlatformResponse process(PlatformService platform) {
        return new PlatformResponse(this);
    }

    @Override // Needed for bootstrap command
    public String tenant() {
        return "";
    }
}
