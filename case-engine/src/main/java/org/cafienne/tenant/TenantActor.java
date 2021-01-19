package org.cafienne.tenant;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.event.TransactionEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.platform.NewUserInformation;
import org.cafienne.cmmn.akka.command.platform.PlatformUpdate;
import org.cafienne.tenant.akka.command.TenantCommand;
import org.cafienne.tenant.akka.command.TenantUserInformation;
import org.cafienne.tenant.akka.event.*;
import org.cafienne.tenant.akka.event.platform.TenantCreated;
import org.cafienne.tenant.akka.event.platform.TenantDisabled;
import org.cafienne.tenant.akka.event.platform.TenantEnabled;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TenantActor extends ModelActor<TenantCommand, TenantEvent> {
    private TenantCreated creationEvent;
    private Map<String, User> users = new HashMap();
    private boolean disabled = false; // TODO: we can add some behavior behind this...

    public TenantActor() {
        super(TenantCommand.class, TenantEvent.class);
    }

    @Override
    public String getParentActorId() {
        return "";
    }

    @Override
    public String getRootActorId() {
        return getId();
    }

    @Override
    public TransactionEvent createTransactionEvent() {
        return new TenantModified(this, getTransactionTimestamp());
    }

    public boolean exists() {
        return this.creationEvent != null;
    }

    public void createInstance(List<TenantUserInformation> newUsers) {
        addEvent(new TenantCreated(this));
        updateInstance(newUsers);
    }

    public void replaceInstance(List<TenantUserInformation> newUsers) {
        new HashMap(users).keySet().forEach(userId -> {
            if (newUsers.stream().filter(user -> user.id().equals(userId)).count() == 0) {
                users.get(userId).disable();
            }
        });
        newUsers.forEach(newUser -> getOrCreate(newUser).replaceWith(newUser));
    }

    public void updateInstance(List<TenantUserInformation> usersToUpdate) {
        usersToUpdate.forEach(this::upsertUser);
    }

    public void updateState(TenantAppliedPlatformUpdate event) {
        event.newUserInformation.info().foreach(userInfo -> {
            User user = users.remove(userInfo.existingUserId());
            users.put(userInfo.newUserId(), user);
            return userInfo;
        });
    }

    public void updateState(TenantCreated tenantCreated) {
        this.setEngineVersion(tenantCreated.engineVersion);
        this.creationEvent = tenantCreated;
    }

    private User getOrCreate(TenantUserInformation userInfo) {
        User existingUser = users.get(userInfo.id());
        if (existingUser == null) {
            addEvent(new TenantUserCreated(this, userInfo.id(), userInfo.getName(), userInfo.getEmail()));
            return getUser(userInfo.id());
        } else {
            return existingUser;
        }
    }

    public void upsertUser(TenantUserInformation newInfo) {
        getOrCreate(newInfo).upsertWith(newInfo);
    }

    public void disable() {
        if (! disabled) {
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

    public User getUser(String userId) {
        return users.get(userId);
    }

    public boolean isOwner(String userId) {
        User user = getUser(userId);
        return user != null && user.isOwner();
    }

    public boolean isOwner(TenantUser user) {
        return isOwner(user.id());
    }

    public List<String> getOwnerList() {
        return users.values().stream().filter(user -> user.isOwner()).map(owner -> owner.userId).collect(Collectors.toList());
    }

    public void updateState(TenantUserCreated event) {
        users.put(event.userId, new User(this, event.userId, event.name, event.email));
    }

    public void updateState(TenantModified event) {
        setLastModified(event.lastModified());
    }

    public void updatePlatformInformation(PlatformUpdate newUserInformation) {
        addEvent(new TenantAppliedPlatformUpdate(this, newUserInformation));
    }
}
