package org.cafienne.tenant;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.akka.event.*;
import scala.collection.Traversable;
import scala.collection.immutable.Seq;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return isOwner && enabled;
    }

    private <T extends TenantUserEvent> T addEvent(T event) {
        return tenant.addEvent(event);
    }

    public void updateFrom(TenantUser newInfo) {
        // First check if name or email has changed.
        if (! (newInfo.name().equalsIgnoreCase(name) && newInfo.email().equalsIgnoreCase(email))) {
            addEvent(new TenantUserUpdated(tenant, userId, newInfo.name(), newInfo.email()));
        }

        // Now loop through the roles and see which ones have to be removed, and which ones have to be added
        Traversable<String> newRoles = newInfo.roles().filter(roleToAdd -> !this.roles.contains(roleToAdd));
        List<String> oldRoles = this.roles.stream().filter(roleToRemove -> !newInfo.roles().contains(roleToRemove)).collect(Collectors.toList());
        newRoles.foreach(role -> addRole(role));
        oldRoles.forEach(role -> removeRole(role));

        // Below functionality is similar to the upsert of a case team member;
        // but for now disabled, as the external akka http interface does not enable it, which leads to false initial values

        // Now check if enabled/disabled changed.
//        if (this.enabled != newInfo.enabled()) {
//            if (newInfo.enabled()) enable();
//            else disable();
//        }

        // Finally check whether user becomes owner or not
//        if (this.isOwner != newInfo.isOwner()) {
//            if (newInfo.isOwner()) makeOwner();
//            else removeOwnership();
//        }
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

    public void updateState(TenantUserUpdated event) {
        this.name = event.name;
        this.email = event.email;
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
