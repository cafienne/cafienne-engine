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

package org.cafienne.userregistration.tenant;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.engine.cmmn.actorapi.command.platform.NewUserInformation;
import org.cafienne.engine.cmmn.actorapi.command.platform.PlatformUpdate;
import org.cafienne.system.CaseSystem;
import org.cafienne.userregistration.tenant.actorapi.command.TenantCommand;
import org.cafienne.userregistration.tenant.actorapi.event.TenantAppliedPlatformUpdate;
import org.cafienne.userregistration.tenant.actorapi.event.TenantEvent;
import org.cafienne.userregistration.tenant.actorapi.event.TenantModified;
import org.cafienne.userregistration.tenant.actorapi.event.deprecated.DeprecatedTenantUserEvent;
import org.cafienne.userregistration.tenant.actorapi.event.platform.TenantCreated;
import org.cafienne.userregistration.tenant.actorapi.event.platform.TenantDisabled;
import org.cafienne.userregistration.tenant.actorapi.event.platform.TenantEnabled;
import org.cafienne.userregistration.tenant.actorapi.event.user.TenantUserAdded;
import org.cafienne.userregistration.tenant.actorapi.event.user.TenantUserChanged;
import org.cafienne.userregistration.tenant.actorapi.event.user.TenantUserRemoved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TenantActor extends ModelActor {
    private final static Logger logger = LoggerFactory.getLogger(TenantActor.class);

    private TenantCreated creationEvent;
    private final Map<String, TenantUser> users = new HashMap<>();
    private boolean disabled = false; // TODO: we can add some behavior behind this...

    public TenantActor(CaseSystem caseSystem) {
        super(caseSystem);
    }

    @Override
    protected boolean supportsCommand(Object msg) {
        return msg instanceof TenantCommand;
    }

    @Override
    protected boolean supportsEvent(ModelEvent msg) {
        return msg instanceof TenantEvent;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public boolean exists() {
        return this.creationEvent != null;
    }

    public void createInstance(List<TenantUser> newUsers) {
        addEvent(new TenantCreated(this));
        replaceInstance(newUsers);
    }

    public void replaceInstance(List<TenantUser> newUsers) {
        // Remove users that no longer exist
        users.keySet().stream().filter(userId -> newUsers.stream().noneMatch(newUser -> newUser.id().equals(userId))).collect(Collectors.toList()).forEach(this::removeUser);
        // Update existing and add new users
        newUsers.forEach(this::setUser);
    }

    public void removeUser(String userId) {
        TenantUser user = users.get(userId);
        if (user != null) {
            addEvent(new TenantUserRemoved(this, user));
        }
    }

    public void setUser(TenantUser newUserInfo) {
        TenantUser existingUser = users.get(newUserInfo.id());
        if (existingUser == null) {
            addEvent(new TenantUserAdded(this, newUserInfo));
        } else {
            if (existingUser.differs(newUserInfo)) {
                Set<String> removedRoles = existingUser.getRoles().stream().filter(role -> !newUserInfo.roles().contains(role)).collect(Collectors.toSet());
                addEvent(new TenantUserChanged(this, newUserInfo, removedRoles));
            }
        }
    }

    public void updateState(TenantAppliedPlatformUpdate event) {
        Map<String, NewUserInformation> updatedUsers = new HashMap<>();
        event.newUserInformation.info().foreach(userInfo -> {
            TenantUser user = users.remove(userInfo.existingUserId());
            if (user != null) {
                users.put(userInfo.newUserId(), userInfo.copyTo(user));
                updatedUsers.put(userInfo.existingUserId(), userInfo);
            } else {
                // Ouch. How can that be? Well ... if same user id is updated multiple times within this event.
                // We'll ignore those updates for now.
                NewUserInformation previouslyUpdated = updatedUsers.get(userInfo.existingUserId());
                if (previouslyUpdated != null) {
                    logger.warn("Not updating user id " + userInfo.existingUserId() + " to " + userInfo.newUserId() + ", because a user with this id has just now been updated to " + previouslyUpdated.newUserId());
                } else {
                    logger.warn("Not updating user id " + userInfo.existingUserId() + " to " + userInfo.newUserId() + ", because a user with this id is not found in the tenant.");
                }
            }
            return userInfo;
        });
    }

    public void updateState(TenantCreated tenantCreated) {
        this.setEngineVersion(tenantCreated.engineVersion);
        this.creationEvent = tenantCreated;
    }

    public void updateState(TenantUserAdded event) {
        users.put(event.memberId, event.member);
    }

    public void updateState(TenantUserChanged event) {
        users.put(event.memberId, event.member);
    }

    public void updateState(TenantUserRemoved event) {
        users.remove(event.memberId);
    }

    public void disable() {
        if (!disabled) {
            addEvent(new TenantDisabled(this));
        }
    }

    public void enable() {
        if (disabled) {
            addEvent(new TenantEnabled(this));
        }
    }

    public void updateState(TenantDisabled event) {
        this.disabled = true;
    }

    public void updateState(TenantEnabled event) {
        this.disabled = false;
    }

    public TenantUser getUser(String userId) {
        return users.get(userId);
    }

    public boolean isOwner(String userId) {
        TenantUser user = getUser(userId);
        return user != null && user.isOwner();
    }

    public boolean isOwner(TenantUser user) {
        return isOwner(user.id());
    }

    public List<String> getOwnerList() {
        return users.values().stream().filter(TenantUser::isOwner).filter(TenantUser::enabled).map(TenantUser::id).collect(Collectors.toList());
    }

    public void updateState(DeprecatedTenantUserEvent event) {
        TenantUser.handleDeprecatedEvent(users, event);
    }

    public void updatePlatformInformation(PlatformUpdate newUserInformation) {
        addEvent(new TenantAppliedPlatformUpdate(this, newUserInformation));
    }

    @Override
    protected void addCommitEvent(IncomingActorMessage message) {
        addEvent(new TenantModified(this, message));
    }
}
