/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.consentgroup.actorapi.command;

import org.cafienne.actormodel.command.BaseModelCommand;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.ConsentGroupUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroupMessage;
import org.cafienne.consentgroup.actorapi.exception.ConsentGroupException;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.json.ValueMap;

/**
 * Consent Groups can be used to invite users from other tenants to join a case tam
 */
public abstract class ConsentGroupCommand extends BaseModelCommand<ConsentGroupActor, ConsentGroupUser> implements ConsentGroupMessage {
    /**
     * Create a new command that can be sent to the group.
     *
     * @param groupOwner The user that issues this command.
     * @param groupId    The id of the consent group
     */
    protected ConsentGroupCommand(ConsentGroupUser groupOwner, String groupId, String rootCaseId) {
        //groupId is the actorId
        super(groupOwner, groupId, rootCaseId);
    }

    protected ConsentGroupCommand(ValueMap json) {
        super(json);
    }

    @Override
    protected ConsentGroupUser readUser(ValueMap json) {
        return ConsentGroupUser.deserialize(json);
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
    public void process(ConsentGroupActor group) {
        processGroupCommand(group);
        if (hasNoResponse()) { // Always return a response
            setResponse(new ConsentGroupResponse(this));
        }
    }

    protected abstract void processGroupCommand(ConsentGroupActor group);
}
