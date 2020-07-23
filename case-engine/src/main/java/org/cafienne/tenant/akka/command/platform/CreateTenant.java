package org.cafienne.tenant.akka.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.command.exception.AuthorizationException;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.platform.TenantCreated;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Manifest
public class CreateTenant extends PlatformTenantCommand implements BootstrapCommand {
    public final String name;
    private final List<TenantUser> users;

    public CreateTenant(PlatformUser user, String tenantId, String name, List<TenantUser> users) {
        super(user, tenantId);
        this.name = name;
        this.users = users;
        // Check whether after the filtering there are still owners left. Tenant must have owners.
        if (this.users.stream().filter(u -> u.isOwner()).count() == 0) {
            throw new AuthorizationException("Cannot create a tenant without providing tenant owners");
        }
    }

    public CreateTenant(ValueMap json) {
        super(json);
        this.name = readField(json, Fields.name);
        this.users = new ArrayList();
        json.withArray(Fields.users).forEach(value -> {
            ValueMap ownerJson = (ValueMap) value;
            this.users.add(TenantUser.from(ownerJson));
        });
    }

    @Override
    public String tenant() {
        return name;
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        if (tenant.exists()) {
            throw new InvalidCommandException("Tenant already exists");
        }
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.addEvent(new TenantCreated(tenant));
        tenant.setInitialUsers(users);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        generator.writeArrayFieldStart(Fields.users.toString());
        for (TenantUser user : users) {
            user.write(generator);
        }
        generator.writeEndArray();
    }
}

