package org.cafienne.tenant;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.event.TransactionEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.akka.command.TenantCommand;
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

    public void setInitialState(TenantCreated tenantCreated) {
        this.setEngineVersion(tenantCreated.engineVersion);
        this.creationEvent = tenantCreated;
    }

    public void setInitialUsers(List<TenantUser> owners) {
        // Register the owners as TenantUsers with the specified roles
        owners.forEach(owner -> createUser(owner, owner.isOwner()));
    }

    private TenantUserCreated createUser(TenantUser user, boolean isOwner) {
        TenantUserCreated event = addEvent(new TenantUserCreated(this, user.id(), user.name(), user.email()));
        User newUser = getUser(user.id());
        user.roles().foreach(role -> newUser.addRole(role));
        if (isOwner) newUser.makeOwner();
        return event;
    }

    public TenantUserCreated createUser(TenantUser user) {
        return createUser(user, false);
    }

    public void upsertUser(TenantUser user) {
        User existingUser = users.get(user.id());
        if (existingUser == null) {
            createUser(user);
        } else {
            existingUser.updateFrom(user);
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
        users.put(event.userId, new User(this, event));
    }

    public void updateState(TenantModified event) {
        setLastModified(event.lastModified());
    }
}
