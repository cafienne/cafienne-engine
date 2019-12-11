package org.cafienne.cmmn.test;

import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.cmmn.test.assertions.PublishedEventsAssertion;

public interface ModelTestCommand<C extends ModelCommand, R extends ModelResponse> {
    int getActionNumber();

    String getActorId();

    C getActualCommand();

    <RESPONSE extends R> RESPONSE getActualResponse();

    /**
     * Returns the list of events published for this test command, as published since by the actor id
     * @return
     */
    default PublishedEventsAssertion<?> getEvents() {
        return getEventListener().getNewEvents().filter(getActorId());
    }

    CommandFailure getActualFailure();

    void handleResponse(R response);

    void handleFailure(CommandFailure failure);

    CaseEventListener getEventListener();

    /**
     * Temporary hack to have some case printing after all for now.
     * @return
     */
    @Deprecated
    default String caseInstanceString() {
        return "No case available";
    }
}
