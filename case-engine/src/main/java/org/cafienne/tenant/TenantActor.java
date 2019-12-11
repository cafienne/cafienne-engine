package org.cafienne.tenant;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.akka.command.TenantCommand;
import org.cafienne.tenant.akka.event.platform.TenantCreated;
import org.cafienne.tenant.akka.event.TenantEvent;
import org.cafienne.tenant.akka.event.platform.TenantDisabled;
import org.cafienne.tenant.akka.event.platform.TenantEnabled;

import java.util.HashSet;
import java.util.Set;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TenantActor extends ModelActor<TenantCommand, TenantEvent> {
    private TenantCreated creationEvent;
    private Set<String> owners = new HashSet<>();
    private boolean disabled = false; // TODO: we can add some behavior behind this...

    public TenantActor() {
        super(TenantCommand.class, TenantEvent.class);
        System.out.println("Created tenant actor with path "+this.self().path());
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
    protected CommandHandler createCommandHandler(TenantCommand msg) {
        return new TenantCommandHandler(this, msg);
    }

    public boolean exists() {
        return this.creationEvent != null;
    }

    public void setInitialState(TenantCreated tenantCreated) {
        this.creationEvent = tenantCreated;
    }

    public void disable(TenantDisabled disabled) {
        this.disabled = true;
    }

    public void enable(TenantEnabled enabled) {
        this.disabled = false;
    }

    public boolean isOwner(String userId) {
        return owners.contains(userId);
    }

    public boolean isOwner(TenantUser user) {
        return isOwner(user.id());
    }

    public void addOwner(String userId) {
        owners.add(userId);
    }

    public void removeOwner(String userId) {
        owners.remove(userId);
    }

    public Set<String> getOwners() {
        return owners;
    }
}
