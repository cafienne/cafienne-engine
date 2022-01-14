package org.cafienne.actormodel.command;

import org.cafienne.actormodel.IncomingActorMessage;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.json.Value;

public interface ModelCommand extends IncomingActorMessage {
    /**
     * Explicit method to be implemented returning the type of the ModelActor handling this message.
     * This is required for the message routing within the CaseSystem
     * @return
     */
    Class<?> actorClass();

    /**
     * Returns the user context for this command.
     *
     * @return
     */
    UserIdentity getUser();

    /**
     * Returns a string with the identifier of the actor towards this command must be sent.
     * @return
     */
    String getActorId();

    default String actorId() {
        return getActorId();
    }

    /**
     * Return the actor handling this command. May return null if setActor() is not yet invoked.
     */
    ModelActor getActor();

    /**
     * Through this method, the command is made aware of the actor that is handling it.
     */
    void setActor(ModelActor actor);

    void validateCommand(ModelActor actor);

    ModelResponse processCommand(ModelActor actor);

    String getCommandDescription();

    /**
     * Return a ValueMap serialization of the command
     */
    Value<?> toJson();
}
