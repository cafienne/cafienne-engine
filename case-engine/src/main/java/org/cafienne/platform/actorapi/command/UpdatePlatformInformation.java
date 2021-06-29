package org.cafienne.platform.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.PlatformUser;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.actorapi.command.platform.CaseUpdate;
import org.cafienne.cmmn.actorapi.command.platform.PlatformUpdate;
import org.cafienne.cmmn.actorapi.command.platform.TenantUpdate;
import org.cafienne.platform.PlatformService;
import org.cafienne.platform.actorapi.response.PlatformResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Manifest
public class UpdatePlatformInformation extends PlatformCommand {
    private final PlatformUpdate platformUpdate;
    public final List<TenantUpdate> tenants;
    public final List<CaseUpdate> cases;

    public UpdatePlatformInformation(PlatformUser user, PlatformUpdate platformUpdate, List<TenantUpdate> tenants, List<CaseUpdate> cases) {
        super(TenantUser.fromPlatformOwner(user, "PLATFORM"));
        this.platformUpdate = platformUpdate;
        this.platformUpdate.validate(); // Let's do an early check on proper input information
        this.tenants = tenants;
        this.cases = cases;
    }

    public UpdatePlatformInformation(ValueMap json) {
        super(json);
        this.platformUpdate = PlatformUpdate.deserialize(json.withArray(Fields.users));
        this.tenants = new ArrayList<>();
        this.cases = new ArrayList<>();
        json.withArray(Fields.tenants).forEach(value -> tenants.add(TenantUpdate.deserialize(value.asMap())));
        json.withArray(Fields.cases).forEach(value -> cases.add(CaseUpdate.deserialize(value.asMap())));
    }

    @Override
    public PlatformResponse process(PlatformService platform) {
        platform.handleUpdate(this);
        return new PlatformResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.users, platformUpdate.toValue());
        writeListField(generator, Fields.tenants, tenants);
        writeListField(generator, Fields.cases, cases);
    }
}

