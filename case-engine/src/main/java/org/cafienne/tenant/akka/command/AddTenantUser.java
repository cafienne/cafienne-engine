package org.cafienne.tenant.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.TenantUserCreated;
import org.cafienne.tenant.akka.event.TenantUserRoleAdded;

import java.io.IOException;
import java.util.Set;

@Manifest
public class AddTenantUser extends TenantCommand {
    public final String userId;
    public final Set<String> roles;
    public final String name;
    public final String email;

    private enum Fields {
        userId, roles, name, email
    }

    public AddTenantUser(TenantUser tenantOwner, String tenantId, String userId, Set<String> roles, String name, String email) {
        super(tenantOwner, tenantId);
        this.userId = userId;
        this.roles = roles;
        this.name = name;
        this.email = email;
    }

    public AddTenantUser(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
        this.roles = readSet(json, Fields.roles);
        this.name = readField(json, Fields.name);
        this.email = readField(json, Fields.email);
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.addEvent(new TenantUserCreated(tenant, userId, name, email)).updateState(tenant);
        // Add all the roles
        roles.forEach(role -> tenant.addEvent(new TenantUserRoleAdded(tenant, userId, role)));
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
        writeField(generator, Fields.roles, roles);
    }
}