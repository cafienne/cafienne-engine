package org.cafienne.actormodel.message;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.CafienneSerializable;

/**
 * A UserMessage carries user information
 * Typically used in Commands and resulting Events and Responses from those commands.
 */
public interface UserMessage extends CafienneSerializable {
    UserIdentity getUser();

    /**
     * Explicit method to be implemented returning the type of the ModelActor handling this message.
     * This is required for the message routing within the CaseSystem
     * @return
     */
    default Class<?> actorClass() {
        return ModelActor.class;
    }

    default boolean isBootstrapMessage() {
        return false;
    }

    default BootstrapMessage asBootstrapMessage() {
        return (BootstrapMessage) this;
    }
}
