package org.cafienne.actormodel.communication.reply.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.communication.reply.command.RunActorRequest;
import org.cafienne.actormodel.communication.request.command.RequestModelActor;
import org.cafienne.actormodel.event.CommitEvent;
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
public class ActorRequestStored extends ModelActorRequestEvent implements CommitEvent {
    public final RequestModelActor request;

    public ActorRequestStored(RequestModelActor request) {
        super(request, request.sourceActorId);
        this.request = request;
    }

    public ActorRequestStored(ValueMap json) {
        super(json);
        this.request = new RequestModelActor(json.readMap(Fields.content));
    }

    @Override
    public void updateState(ModelActor actor) {
        super.updateState(actor);
        if (request.command.isBootstrapMessage()) {
            actor.setDebugMode(request.debugMode);
        }
    }

    @Override
    public void afterPersist(ModelActor actor) {
        // After event is persisted we send it to ourselves, to trigger the actual command handling.
        actor.self().tell(new RunActorRequest(this), actor.sender());
    }

    @Override
    public String getMessageId() {
        return request.getMessageId();
    }

    @Override
    public String getDescription() {
        return super.getDescription() + "[" + request.command.getDescription() + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeIncomingRequestEvent(generator);
        generator.writeFieldName(Fields.content.toString());
        request.writeThisObject(generator);
    }
}
