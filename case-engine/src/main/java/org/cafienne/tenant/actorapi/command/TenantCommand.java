/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.actorapi.command;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.command.exception.AuthorizationException;
import org.cafienne.actormodel.command.exception.CommandException;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.exception.TenantException;

import java.util.List;

/**
 * A {@link Case} instance is designed to handle various AkkaCaseCommands, such as {@link StartCase}, {@link MakePlanItemTransition}, etc.
 * Each CaseCommand must implement it's own logic within the case, through the optional {@link ModelCommand#validate} and the mandatory {@link TenantCommand#process} methods.
 * When the case has succesfully handled the command, it will persist the resulting {@link CaseEvent}s, and send a reply back, see {@link CaseResponse}.
 */
public abstract class TenantCommand extends ModelCommand<TenantActor> {
    /**
     * Create a new command that can be sent to the case.
     *  @param tenantOwner The user that issues this command.
     *
     */
    protected TenantCommand(TenantUser tenantOwner) {
        super(tenantOwner, tenantOwner.tenant());
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

    protected void validateNotLastOwner(TenantActor tenant, TenantUserInformation newUser) {
        // If either
        // 1. ownership is defined and revoked in the new information (needs to be checked like this, because isOwner() defaults to false)
        // 2. or if the account is no longer enabled (isEnabled defaults to true, so if false it must have been set to change)
        // Then
        //  check whether this user is the last man standing in the list of owners. If so, the command cannot be executed.
        if ((newUser.owner().nonEmpty() && !newUser.isOwner()) || !newUser.isEnabled()) {
            List<String> currentOwners = tenant.getOwnerList();
            // If only 1 owner, and newUser has the same id, then throw the exception
            if (currentOwners.size() == 1 && currentOwners.contains(newUser.id())) {
                throw new TenantException("Cannot remove tenant ownership or disable the account. There must be at least one tenant owner.");
            }
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
