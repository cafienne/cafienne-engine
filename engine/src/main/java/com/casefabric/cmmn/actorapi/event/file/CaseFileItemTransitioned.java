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

package com.casefabric.cmmn.actorapi.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.casefile.CaseFileItem;
import com.casefabric.cmmn.instance.casefile.CaseFileItemCollection;
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition;
import com.casefabric.cmmn.instance.casefile.InvalidPathException;
import com.casefabric.cmmn.instance.sentry.StandardEvent;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Event caused by a transition on a CaseFileItem
 */
@Manifest(value = "CaseFileEvent") // Need this manifest to be compatible with before version 1.1.9 events
public class CaseFileItemTransitioned extends CaseFileEvent implements StandardEvent<CaseFileItemTransition, CaseFileItem> {
    protected final static Logger logger = LoggerFactory.getLogger(CaseFileItemTransitioned.class);

    private final CaseFileItemTransition transition;
    private final Value<?> value;
    private final State state;

    public CaseFileItemTransitioned(CaseFileItemCollection<?> item, State newState, CaseFileItemTransition transition, Value<?> newValue) {
        super(item);
        this.transition = transition;
        this.value = newValue;
        this.state = newState;
    }

    public CaseFileItemTransitioned(ValueMap json) {
        super(json);
        this.transition = json.readEnum(Fields.transition, CaseFileItemTransition.class);
        this.value = json.get(Fields.value.toString());
        this.state = json.readEnum(Fields.state, State.class);
    }

    @Override
    public CaseFileItem getSource() {
        return caseFileItem;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName() + "['" + path + "']." + getTransition().toString().toLowerCase() + "() ===> " + getState();
    }

    /**
     * Returns the transition that the case file item went through.
     *
     * @return
     */
    public CaseFileItemTransition getTransition() {
        return transition;
    }

    /**
     * Returns the state of the case file item
     *
     * @return
     */
    public State getState() {
        return state;
    }

    /**
     * Returns the new value of the case file item.
     *
     * @return
     */
    public Value<?> getValue() {
        return value;
    }

    @Override
    protected void updateState(CaseFileItem item) {
        item.publishTransition(this);
    }

    @Override
    public boolean hasBehavior() {
        return true;
    }

    @Override
    public void runImmediateBehavior() {
        caseFileItem.informConnectedEntryCriteria(this);
    }

    @Override
    public void runDelayedBehavior() {
        caseFileItem.informConnectedExitCriteria(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseFileEvent(generator);
        writeField(generator, Fields.transition, transition);
        writeField(generator, Fields.value, value);
        writeField(generator, Fields.state, state);
    }
}
