package org.cafienne.tenant;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.akka.command.TenantCommand;
import org.cafienne.tenant.akka.event.*;
import org.cafienne.tenant.akka.event.platform.TenantCreated;
import org.cafienne.tenant.akka.event.platform.TenantDisabled;
import org.cafienne.tenant.akka.event.platform.TenantEnabled;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TenantActor extends ModelActor<TenantCommand, TenantEvent> {
    private TenantCreated creationEvent;
    private Set<String> owners = new HashSet<>();
    private Map<String, InternalTenantUserRepresentation> users = new HashMap<>();
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
    public TenantModified createLastModifiedEvent(Instant lastModified) {
        return new TenantModified(this, lastModified);
    }

    public boolean exists() {
        return this.creationEvent != null;
    }

    public void setInitialState(TenantCreated tenantCreated) {
        this.setEngineVersion(tenantCreated.engineVersion);
        this.creationEvent = tenantCreated;
    }

    public void updateState(TenantDisabled event) {
        this.disabled = true;
    }

    public void updateState(TenantEnabled event) {
        this.disabled = false;
    }

    public boolean isUser(String userId) {
        return users.containsKey(userId);
    }

    public boolean isOwner(String userId) {
        return owners.contains(userId);
    }

    public boolean isOwner(TenantUser user) {
        return isOwner(user.id());
    }

    public Set<String> getOwners() {
        return owners;
    }

    public void updateState(TenantUserCreated event) {
        users.put(event.userId, new InternalTenantUserRepresentation(event));
    }

    public void updateState(OwnerRemoved event) {
        owners.remove(event.userId);
    }

    public void updateState(OwnerAdded event) {
        owners.add(event.userId);
    }

    public void updateState(TenantUserEnabled event) {
        users.get(event.userId).enabled = true;
    }

    public void updateState(TenantUserDisabled event) {
        users.get(event.userId).enabled = false;
    }

    public void updateState(TenantUserRoleAdded event) {
        users.get(event.userId).roles.add(event.role);
    }

    public void updateState(TenantUserRoleRemoved event) {
        users.get(event.userId).roles.remove(event.role);
    }

    public void updateState(TenantModified event) {
        setLastModified(event.lastModified());
    }
}

class InternalTenantUserRepresentation {
    final String userId;
    final Set<String> roles = new HashSet<>();
    boolean enabled = true;
    String name;
    String email;

    InternalTenantUserRepresentation(TenantUserCreated event) {
        this.userId = event.userId;
        this.name = event.name;
        this.email = event.email;
    }
}