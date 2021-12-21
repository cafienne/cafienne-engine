/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.consentgroup.actorapi.command;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.event.ConsentGroupModified;
import org.cafienne.consentgroup.actorapi.exception.ConsentGroupException;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.json.ValueMap;

/**
 * Consent Groups can be used to invite users from other tenants to join a case tam
 */
public abstract class ConsentGroupCommand extends ModelCommand<ConsentGroupActor, TenantUser> {
    /**
     * Create a new command that can be sent to the group.
     *
     * @param groupOwner The user that issues this command.
     * @param groupId    The id of the consent group
     */
    protected ConsentGroupCommand(TenantUser groupOwner, String groupId) {
        super(groupOwner, groupId);
    }

    protected ConsentGroupCommand(ValueMap json) {
        super(json);
    }

    @Override
    protected TenantUser readUser(ValueMap json) {
        return TenantUser.deserialize(json);
    }

    @Override
    public final Class<ConsentGroupActor> actorClass() {
        return ConsentGroupActor.class;
    }

    /**
     * Before the group starts processing the command, it will first ask to validate the command.
     * The default implementation is to check whether the case definition is available (i.e., whether StartCase command has been triggered before this command).
     * Implementations can override this method to implement their own validation logic.
     * Implementations may throw the {@link InvalidCommandException} if they encounter a validation error
     *
     * @param group
     * @throws InvalidCommandException If the command is invalid
     */
    public void validate(ConsentGroupActor group) throws InvalidCommandException {
        // Group must exist
        if (!group.exists()) {
            throw new ConsentGroupException("Not allowed to access this consent group");
        }

        if (!group.isOwner(this.getUser())) {
            throw new AuthorizationException("You do not have the privileges to perform this action");
        }
    }

    protected void validateNotLastMember(ConsentGroupActor group, String userId) {
        if (group.getMembers().size() == 1) {
            throw new ConsentGroupException("Cannot remove group membership for user " + userId + ". There must be at least one member.");
        }
    }

    protected void validateNotLastOwner(ConsentGroupActor group, String userId) {
        if (group.getOwners().size() == 1 && group.getOwners().contains(userId)) {
            throw new ConsentGroupException("Cannot remove group ownership of user " + userId + ". There must be at least one group owner.");
        }
    }

    @Override
    public void done() {
        actor.addEvent(new ConsentGroupModified(this));
    }
}
