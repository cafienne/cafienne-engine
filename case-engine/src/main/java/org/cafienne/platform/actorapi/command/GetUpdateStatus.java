package org.cafienne.platform.actorapi.command;

import org.cafienne.actormodel.identity.PlatformUser;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.platform.PlatformService;
import org.cafienne.platform.actorapi.response.PlatformUpdateStatus;

@Manifest
public class GetUpdateStatus extends PlatformCommand {
    public GetUpdateStatus(PlatformUser user) {
        super(TenantUser.fromPlatformOwner(user, "PLATFORM"));
    }

    public GetUpdateStatus(ValueMap json) {
        super(json);
    }

    @Override
    public PlatformUpdateStatus process(PlatformService platform) {
        return platform.getUpdateStatus(this);
    }
}

