package org.cafienne.tenant.actorapi.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.actorapi.command.GetTenantOwners;

import java.io.IOException;
import java.util.List;

@Manifest
public class TenantOwnersResponse extends TenantResponse {
    public final String name;
    public final List<String> owners;

    public TenantOwnersResponse(GetTenantOwners command, String name, List<String> owners) {
        super(command);
        this.name = name;
        this.owners = owners;
    }

    public TenantOwnersResponse(ValueMap json) {
        super(json);
        this.name = readField(json, Fields.name);
        this.owners = json.withArray(Fields.owners).rawList();
    }

    @Override
    public Value<?> toJson() {
        return Value.convert(owners);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.owners, owners);
    }
}
