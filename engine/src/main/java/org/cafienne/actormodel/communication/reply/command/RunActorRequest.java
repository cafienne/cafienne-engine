package org.cafienne.actormodel.communication.reply.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.communication.CaseSystemCommunicationCommand;
import org.cafienne.actormodel.communication.reply.event.ActorRequestExecuted;
import org.cafienne.actormodel.communication.reply.event.ActorRequestStored;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * ActorRequests are sent between ModelActors. When a request is received by an actor,
 * it is stored as an ActorRequestReceived event, which is also a CommitEvent,
 * as in the event itself did not really make a functional change to the ModelActor.
 * This only happens when the ModelActor starts to handle the command that is stored inside the event.
 * This is done by telling actor.self() about the event - which is done only after the event was persisted.
 */
@Manifest
public class RunActorRequest extends CaseSystemCommunicationCommand {
    public final String sourceActorId;

    public RunActorRequest(ActorRequestStored event) {
        super(event.request.command);
        this.sourceActorId = event.sourceActorId;
    }

    public RunActorRequest(ValueMap json) {
        super(json);
        this.sourceActorId = json.readString(Fields.sourceActorId);
    }

    @Override
    public void setActor(ModelActor actor) {
        // Setting the actor must also be done on the command we carry
        super.setActor(actor);
        command.setActor(actor);
    }

    @Override
    public void validate(ModelActor modelActor) throws InvalidCommandException {
        command.validateCommand(actor);
    }

    @Override
    public void process(ModelActor modelActor) {
        command.processCommand(actor);
        actor.addEvent(new ActorRequestExecuted(command, sourceActorId));
    }

    @Override
    public String getCommandDescription() {
        return "Run[" + command.getDescription() + "]";
    }

    @Override
    public ModelResponse getResponse() {
//        System.out.println(this.getDescription() + ": No need to return a response from command handling");
//        return new ActorRequestExecuted(request, request.command().getResponse());
        return null;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeActorCommand(generator);
        writeField(generator, Fields.sourceActorId, sourceActorId);
    }
}
