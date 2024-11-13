/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.event.ActorModified;
import com.casefabric.actormodel.message.IncomingActorMessage;
import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.State;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.LongValue;
import com.casefabric.json.ValueMap;

import java.io.IOException;

/**
 * Event that is published after an {@link CaseCommand} has been fully handled by a {@link Case} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class CaseModified extends ActorModified<Case> implements CaseEvent {
    private final int numFailures;
    private final State state;

    public CaseModified(Case caseInstance, IncomingActorMessage source, int numFailures) {
        super(caseInstance, source);
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
