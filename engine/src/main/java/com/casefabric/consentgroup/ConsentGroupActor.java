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

package com.casefabric.consentgroup;

import com.casefabric.actormodel.ModelActor;
import com.casefabric.actormodel.event.ModelEvent;
import com.casefabric.actormodel.identity.ConsentGroupUser;
import com.casefabric.actormodel.message.IncomingActorMessage;
import com.casefabric.consentgroup.actorapi.ConsentGroupMember;
import com.casefabric.consentgroup.actorapi.command.ConsentGroupCommand;
import com.casefabric.consentgroup.actorapi.command.CreateConsentGroup;
import com.casefabric.consentgroup.actorapi.command.ReplaceConsentGroup;
import com.casefabric.consentgroup.actorapi.event.*;
import com.casefabric.system.CaseSystem;

import java.util.*;
import java.util.stream.Collectors;

public class ConsentGroupActor extends ModelActor {
    private final Map<String, ConsentGroupMember> members = new HashMap<>();
    private boolean created = false;

    public ConsentGroupActor(CaseSystem caseSystem) {
        super(caseSystem);
    }

    @Override
    protected boolean supportsCommand(Object msg) {
        return msg instanceof ConsentGroupCommand;
    }

    @Override
    protected boolean supportsEvent(ModelEvent msg) {
        return msg instanceof ConsentGroupEvent;
    }

    public boolean exists() {
        return created;
    }

    public boolean isOwner(ConsentGroupUser user) {
        return members.values().stream().filter(ConsentGroupMember::isOwner).anyMatch(member -> member.userId().equals(user.id()));
    }

    public List<String> getOwners() {
        return members.values().stream().filter(ConsentGroupMember::isOwner).map(ConsentGroupMember::userId).collect(Collectors.toList());
    }

    public Collection<ConsentGroupMember> getMembers() {
        return members.values();
    }

    public ConsentGroupMember getMember(String userId) {
        return members.get(userId);
    }

    public boolean setMember(ConsentGroupMember newInfo) {
        ConsentGroupMember existingUser = members.get(newInfo.userId());
        if (existingUser == null) {
            addEvent(new ConsentGroupMemberAdded(this, newInfo));
        } else {
            // First check if there are changes.
            boolean ownershipChanged = newInfo.isOwner() != existingUser.isOwner();
            Set<String> rolesRemoved = existingUser.getRoles().stream().filter(newRole -> !newInfo.getRoles().contains(newRole)).collect(Collectors.toSet());
            Set<String> rolesAdded = newInfo.getRoles().stream().filter(newRole -> !existingUser.getRoles().contains(newRole)).collect(Collectors.toSet());
            if (ownershipChanged || !rolesRemoved.isEmpty() || !rolesAdded.isEmpty()) {
                addEvent(new ConsentGroupMemberChanged(this, newInfo, rolesRemoved));
            }
        }
        return true;
    }

    public void removeMember(String userId) {
        ConsentGroupMember member = members.remove(userId);
        if (member != null) {
            addEvent(new ConsentGroupMemberRemoved(this, member));
        }
    }

    public void updateState(ConsentGroupMemberAdded event) {
        members.put(event.userId, event.member);
    }

    public void updateState(ConsentGroupMemberChanged event) {
        members.put(event.userId, event.member);
    }

    public void updateState(ConsentGroupMemberRemoved event) {
        members.remove(event.userId);
    }

    public void updateState(ConsentGroupCreated event) {
        this.created = true;
        this.setEngineVersion(event.engineVersion);
    }

    public void create(CreateConsentGroup command) {
        addEvent(new ConsentGroupCreated(this, command.tenant()));
        command.getMembers().foreach(this::setMember);
    }

    public void replace(ReplaceConsentGroup command) {
        // Remove users that no longer exist
        members.keySet().stream().filter(command::missingUserId).collect(Collectors.toList()).forEach(this::removeMember);
        command.getMembers().foreach(this::setMember);
    }

    @Override
    protected void addCommitEvent(IncomingActorMessage message) {
        addEvent(new ConsentGroupModified(this, message));
    }
}
