package org.cafienne.platform.actorapi.command;

import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
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

