package org.cafienne.tenant.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.exception.TenantException;
import org.cafienne.tenant.actorapi.response.TenantResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Manifest
public class UpdateTenant extends TenantCommand {
    private final List<TenantUserInformation> users;

    public UpdateTenant(TenantUser tenantOwner, String tenant, List<TenantUserInformation> users) {
        super(tenantOwner, tenant);
        this.users = users;
    }

    public UpdateTenant(ValueMap json) {
        super(json);
        this.users = json.readObjects(Fields.users, TenantUserInformation::from);
    }

    @Override
    public void validate(TenantActor tenant) throws InvalidCommandException {
        super.validate(tenant);
        // Check whether after the filtering there are still owners left. Tenant must have owners.
        Stream<String> currentOwners = tenant.getOwnerList().stream();
        List<String> userIdsThatWillBeUpdated = this.users.stream().map(TenantUserInformation::id).collect(Collectors.toList());
        long untouchedOwners = currentOwners.filter(currentOwner -> !userIdsThatWillBeUpdated.contains(currentOwner)).count();
        if (untouchedOwners == 0 && this.users.stream().noneMatch(u -> u.isOwner() && u.isEnabled())) {
            throw new TenantException("Cannot update the tenant and remove all tenant owners or disable their accounts");
        }
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        tenant.updateInstance(users);
        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeListField(generator, Fields.users, users);
    }
}
