package org.cafienne.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.ActorModified;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.State;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.LongValue;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Event that is published after an {@link CaseCommand} has been fully handled by a {@link Case} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class CaseModified extends ActorModified implements CaseEvent {
    private final int numFailures;
    private final State state;

    public CaseModified(CaseCommand command, Case caseInstance, int numFailures) {
        super(command);
        this.numFailures = numFailures;
        this.state = caseInstance.getCasePlan().getState();
    }

    public CaseModified(ValueMap json) {
        super(json);
        this.numFailures = json.rawInt(Fields.numFailures);
        this.state = json.readEnum(Fields.state, State.class);
    }

    /**
     * Returns the state that the case plan currently has
     * @return
     */
    public State getState() {
        return this.state;
    }

    /**
     * Returns the number of plan items within the case in state Failed.
     * @return
     */
    public int getNumFailures() {
        return numFailures;
    }

    @Override
    public String toString() {
        return "CaseModified[" + getCaseInstanceId() + "] at " + lastModified();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeActorModified(generator);
        writeField(generator, Fields.numFailures, new LongValue(numFailures));
        writeField(generator, Fields.state, state);
    }
}
