package org.cafienne.tenant;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation;
import org.cafienne.cmmn.actorapi.command.platform.PlatformUpdate;
import org.cafienne.system.CaseSystem;
import org.cafienne.tenant.actorapi.command.TenantCommand;
import org.cafienne.tenant.actorapi.command.TenantUserInformation;
import org.cafienne.tenant.actorapi.event.TenantAppliedPlatformUpdate;
import org.cafienne.tenant.actorapi.event.TenantEvent;
import org.cafienne.tenant.actorapi.event.TenantModified;
import org.cafienne.tenant.actorapi.event.TenantUserCreated;
import org.cafienne.tenant.actorapi.event.platform.TenantCreated;
import org.cafienne.tenant.actorapi.event.platform.TenantDisabled;
import org.cafienne.tenant.actorapi.event.platform.TenantEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TenantActor extends ModelActor<TenantCommand, TenantEvent> {
    private final static Logger logger = LoggerFactory.getLogger(TenantActor.class);

    private TenantCreated creationEvent;
    private final Map<String, User> users = new HashMap<>();
    private boolean disabled = false; // TODO: we can add some behavior behind this...

    public TenantActor(CaseSystem caseSystem) {
        super(TenantCommand.class, TenantEvent.class, caseSystem);
    }

    @Override
    public String getDescription() {
        return "Tenant[" + getId() + "]";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public boolean exists() {
        return this.creationEvent != null;
    }

    public void createInstance(List<TenantUserInformation> newUsers) {
        addEvent(new TenantCreated(this));
        updateInstance(newUsers);
    }

    public void replaceInstance(List<TenantUserInformation> newUsers) {
        new HashMap<>(users).keySet().forEach(userId -> {
            if (newUsers.stream().noneMatch(user -> user.id().equals(userId))) {
                users.get(userId).disable();
            }
        });
        newUsers.forEach(newUser -> getOrCreate(newUser).replaceWith(newUser));
    }

    public void updateInstance(List<TenantUserInformation> usersToUpdate) {
        usersToUpdate.forEach(this::upsertUser);
    }

    public void updateState(TenantAppliedPlatformUpdate event) {
        Map<String, NewUserInformation> updatedUsers = new HashMap<>();
        event.newUserInformation.info().foreach(userInfo -> {
            User user = users.remove(userInfo.existingUserId());
            if (user != null) {
                users.put(userInfo.newUserId(), user.copy(userInfo.newUserId()));
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
        return users.values().stream().filter(User::isOwner).map(owner -> owner.userId).collect(Collectors.toList());
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
