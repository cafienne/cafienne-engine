package org.cafienne.tenant;

import org.cafienne.tenant.akka.command.TenantUserInformation;
import org.cafienne.tenant.akka.event.*;
import scala.collection.Seq;
import scala.collection.Traversable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class User {
    final String userId;
    private final Set<String> roles = new HashSet();
    private boolean isOwner = false;
    private boolean enabled = true;
    private String name;
    private String email;

    private final TenantActor tenant;

    User(TenantActor tenant, String userId, String name, String email) {
        this.tenant = tenant;
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    /**
     * Create an exact copy of the user, but then with the new user id.
     * @param newUserId
     * @return
     */
    User copy(String newUserId) {
        User copy = new User(this.tenant, newUserId, this.name, this.email);
        copy.roles.addAll(this.roles);
        copy.enabled = this.enabled;
        copy.isOwner = this.isOwner;
        return copy;
    }

    public boolean isOwner() {
        return isOwner && enabled;
    }

    private <T extends TenantUserEvent> T addEvent(T event) {
        return tenant.addEvent(event);
    }

    private void updateNameAndEmail(String newName, String newEmail) {
        if (!(newName.equalsIgnoreCase(name) && newEmail.equalsIgnoreCase(email))) {
            addEvent(new TenantUserUpdated(tenant, userId, newName, newEmail));
        }
    }

    private void updateRoles(Seq<String> newRolesInfo) {
        Traversable<String> newRolesToAdd = newRolesInfo.filter(roleToAdd -> !this.roles.contains(roleToAdd));
        List<String> oldRolesToRemove = this.roles.stream().filter(roleToRemove -> !newRolesInfo.contains(roleToRemove)).collect(Collectors.toList());

        newRolesToAdd.foreach(role -> addRole(role));
        oldRolesToRemove.forEach(role -> removeRole(role));
    }

    private void updateAccountEnabled(boolean newEnabled) {
        if (this.enabled != newEnabled) {
            if (newEnabled) this.enable();
            else this.disable();
        }
    }

    private void updateOwnership(boolean newOwnership) {
        if (this.isOwner != newOwnership) {
            if (newOwnership) this.makeOwner();
            else this.removeOwnership();
        }
    }

    /**
     * Replaces existing user information (but only if it has changed)
     * @param newInfo
     */
    public void replaceWith(TenantUserInformation newInfo) {
        updateNameAndEmail(newInfo.getName(), newInfo.getEmail());
        updateRoles(newInfo.getRoles());
        updateAccountEnabled(newInfo.isEnabled());
        updateOwnership(newInfo.isOwner());
    }

    /**
     * Updates only information if it is defined in the new information
     * @param newInfo
     */
    public void upsertWith(TenantUserInformation newInfo) {
        // First check if name or email has changed.
        if (newInfo.name().nonEmpty() || newInfo.email().nonEmpty()) {
            String newName = newInfo.name().nonEmpty() ? newInfo.name().get() : this.name;
            String newEmail = newInfo.email().nonEmpty() ? newInfo.email().get() : this.email;
            updateNameAndEmail(newName, newEmail);
        }

        // Check to see if roles must be updated
        if (newInfo.roles().nonEmpty()) {
            updateRoles(newInfo.getRoles());
        }

        // Now check if enabled/disabled changed.
        if (newInfo.enabled().nonEmpty()) {
            updateAccountEnabled(newInfo.isEnabled());
        }

        // Finally check whether ownership must be changed
        if (newInfo.owner().nonEmpty()) {
            updateOwnership(newInfo.isOwner());
        }
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
