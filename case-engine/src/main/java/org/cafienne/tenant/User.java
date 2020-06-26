package org.cafienne.tenant;

import org.cafienne.tenant.akka.event.*;

import java.util.HashSet;
import java.util.Set;

public class User {
    final String userId;
    private final Set<String> roles = new HashSet();
    private boolean isOwner = false;
    private boolean enabled = true;
    private String name;
    private String email;

    private final TenantActor tenant;

    User(TenantActor tenant, TenantUserCreated event) {
        this.tenant = tenant;
        this.userId = event.userId;
        this.name = event.name;
        this.email = event.email;
    }

    public boolean isOwner() {
        return isOwner;
    }

    private <T extends TenantUserEvent> T addEvent(T event) {
        return tenant.addEvent(event);
    }

    public TenantUserRoleRemoved removeRole(String role) {
        return addEvent(new TenantUserRoleRemoved(tenant, userId, role));
    }

    public TenantUserRoleAdded addRole(String role) {
        return addEvent(new TenantUserRoleAdded(tenant, userId, role));
    }

    public void disable() {
        addEvent(new TenantUserDisabled(tenant, userId));
    }

    public void enable() {
        addEvent(new TenantUserEnabled(tenant, userId));
    }

    public OwnerAdded makeOwner() {
        return addEvent(new OwnerAdded(tenant, userId));
    }

    public OwnerRemoved removeOwnership() {
        return addEvent(new OwnerRemoved(tenant, userId));
    }

    public void updateState(TenantUserRoleAdded event) {
        roles.add(event.role);
    }

    public void updateState(TenantUserRoleRemoved event) {
        roles.remove(event.role);
    }

    public void updateState(TenantUserDisabled event) {
        enabled = false;
    }

    public void updateState(TenantUserEnabled event) {
        enabled = true;
    }

    public void updateState(OwnerRemoved event) {
        isOwner = false;
    }

    public void updateState(OwnerAdded event) {
        isOwner = true;
    }
}
