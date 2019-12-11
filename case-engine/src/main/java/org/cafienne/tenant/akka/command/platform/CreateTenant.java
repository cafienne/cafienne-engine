package org.cafienne.tenant.akka.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueList;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.OwnerAdded;
import org.cafienne.tenant.akka.event.TenantUserCreated;
import org.cafienne.tenant.akka.event.TenantUserRoleAdded;
import org.cafienne.tenant.akka.event.platform.TenantCreated;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Manifest
public class CreateTenant extends PlatformTenantCommand implements BootstrapCommand {
    public final String name;
    private final Set<TenantUser> owners;

    private enum Fields {
        name, owners
    }

    public CreateTenant(PlatformUser user, String tenantId, String name, Set<TenantUser> owners) {
        super(user, tenantId);
        this.name = name;
        // Filter out empty and null user id's for the set of owners.
        this.owners = owners;
        // Check whether after the filtering there are still owners left. Tenant must have owners.
        if (this.owners.isEmpty()) {
            throw new SecurityException("Cannot create a tenant without providing tenant owners");
        }
    }

    public CreateTenant(ValueMap json) {
        super(json);
        this.name = readField(json, Fields.name);
        this.owners = new HashSet<>();
        ValueList jsonOwners = json.withArray(Fields.owners);
        jsonOwners.forEach(value -> {
            ValueMap ownerJson = (ValueMap) value;
            this.owners.add(TenantUser.from(ownerJson));
        });
    }

    @Override
    public String tenant() {
        return name;
    }

    @Override
    public void validate(TenantActor modelActor) throws InvalidCommandException {
        if (modelActor.exists()) {
            throw new InvalidCommandException("Tenant already exists");
        }
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.addEvent(new TenantCreated(tenant)).updateState(tenant);
        // Register the owners as TenantUsers with the specified roles
        owners.forEach(owner -> {
            tenant.addEvent(new TenantUserCreated(tenant, owner.id(), owner.name(), owner.email())).updateState(tenant);
            // Add all the roles
            owner.roles().foreach(role -> tenant.addEvent(new TenantUserRoleAdded(tenant, owner.id(), role)));
        });

        // Register the owners as TenantOwners
        owners.forEach(owner -> tenant.addEvent(new OwnerAdded(tenant, owner.id())).updateState(tenant));
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        generator.writeArrayFieldStart(Fields.owners.toString());
        for (TenantUser owner : owners) {
            owner.write(generator);
        }
        generator.writeEndArray();
    }
}

