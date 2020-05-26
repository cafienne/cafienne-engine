package org.cafienne.tenant.akka.command.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.tenant.akka.command.GetTenantOwners;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Manifest
public class TenantOwnersResponse extends TenantResponse {
    public final String name;
    public final Set<String> owners;

    private enum Fields {
        name, owners
    }

    public TenantOwnersResponse(GetTenantOwners command, String name, Set<String> owners) {
        super(command);
        this.name = name;
        this.owners = owners;
    }

    public TenantOwnersResponse(ValueMap json) {
        super(json);
        this.name = readField(json, Fields.name);
        this.owners = new HashSet();
        readArray(json, Fields.owners).getValue().forEach(v -> owners.add(String.valueOf(v.getValue())));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.owners, owners);
    }
}
