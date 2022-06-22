package org.cafienne.actormodel.command;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.json.ValueMap;

public interface ModelCommand extends IncomingActorMessage {
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
    ValueMap rawJson();

    @Override
    default boolean isCommand() {
        return true;
    }

    @Override
    default ModelCommand asCommand() {
        return this;
    }
}
