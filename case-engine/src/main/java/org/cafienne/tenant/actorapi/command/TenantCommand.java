/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.tenant.actorapi.command;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.event.TenantModified;
import org.cafienne.tenant.actorapi.exception.TenantException;

import java.util.List;

/**
 * Base class for sending commands to a TenantActor
 */
public abstract class TenantCommand extends ModelCommand<TenantActor, TenantUser> {
    /**
     * Create a new command that can be sent to the tenant.
     *
     * @param tenantOwner The user that issues this command.
     * @param tenantId    Id of the tenant to send the command to
     */
    protected TenantCommand(TenantUser tenantOwner, String tenantId) {
        super(tenantOwner, tenantId);
    }

    protected TenantCommand(ValueMap json) {
        super(json);
    }

    @Override
    protected TenantUser readUser(ValueMap json) {
        return TenantUser.deserialize(json);
    }

    @Override
    public final Class<TenantActor> actorClass() {
        return TenantActor.class;
    }

    /**
     * Hook to validate the command.
     *
     * @param tenant
     * @throws InvalidCommandException If the command is invalid
     */
    public void validate(TenantActor tenant) throws InvalidCommandException {
        // Tenant must exist
        if (!tenant.exists()) {
            throw new TenantException("Not allowed to access this tenant");
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

    @Override
    public void done() {
        actor.addEvent(new TenantModified(this));
    }
}
