package org.cafienne.platform.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.platform.CaseUpdate;
import org.cafienne.cmmn.akka.command.platform.PlatformUpdate;
import org.cafienne.cmmn.akka.command.platform.TenantUpdate;
import org.cafienne.platform.PlatformService;
import org.cafienne.platform.akka.response.PlatformResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Manifest
public class UpdatePlatformInformation extends PlatformCommand {
    private final PlatformUpdate platformUpdate;
    private final List<TenantUpdate> tenants;
    private final List<CaseUpdate> cases;

    public UpdatePlatformInformation(PlatformUser user, PlatformUpdate platformUpdate, List<TenantUpdate> tenants, List<CaseUpdate> cases) {
        super(TenantUser.fromPlatformOwner(user, "PLATFORM"));
        this.platformUpdate = platformUpdate;
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
        try {
            platform.handleNewInformation(this.user, this.platformUpdate, this.tenants, this.cases);

        } catch (Throwable t) {
            t.printStackTrace();;
        }
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

