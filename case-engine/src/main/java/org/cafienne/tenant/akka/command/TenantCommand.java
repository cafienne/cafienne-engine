/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.akka.command;

import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.AuthorizationException;
import org.cafienne.akka.actor.command.exception.CommandException;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.exception.MissingTenantException;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.exception.TenantException;

/**
 * A {@link Case} instance is designed to handle various AkkaCaseCommands, such as {@link StartCase}, {@link MakePlanItemTransition}, etc.
 * Each CaseCommand must implement it's own logic within the case, through the optional {@link ModelCommand#validate} and the mandatory {@link TenantCommand#process} methods.
 * When the case has succesfully handled the command, it will persist the resulting {@link CaseEvent}s, and send a reply back, see {@link CaseResponse}.
 */
public abstract class TenantCommand extends ModelCommand<TenantActor> {
    /**
     * Create a new command that can be sent to the case.
     *
     * @param tenantOwner The user that issues this command.
     * @param tenantId The id of the case in which to perform this command.
     * @throws MissingTenantException If the user context does not have a tenant field.
     */
    protected TenantCommand(TenantUser tenantOwner, String tenantId) {
        super(tenantOwner, tenantId);
    }

    protected TenantCommand(ValueMap json) {
        super(json);
    }

    @Override
    public final Class<TenantActor> actorClass() {
        return TenantActor.class;
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
    public void validate(TenantActor tenant) throws InvalidCommandException {
        // Tenant must exist
        if (!tenant.exists()) {
            throw new TenantException("Not allowed to access this tenant from " + getUser().tenant());
//            throw new SecurityException("This tenant does not exist");
        }

        // User must do it in the right tenant
        if (!tenant.getTenant().equals(getUser().tenant())) {
            throw new TenantException("Not allowed to access this tenant from " + getUser().tenant());
        }

        if (!tenant.isOwner(this.getUser())) {
            throw new AuthorizationException("You do not have the privileges to perform this action");
        }
    }

    /**
     * Method invoked by the case in order to perform the actual command logic on the case.
     *
     * @param tenant
     * @return
     * @throws CommandException Implementations of this method may throw this exception if a failure happens while processing the command
     */
    public abstract ModelResponse process(TenantActor tenant);
}
