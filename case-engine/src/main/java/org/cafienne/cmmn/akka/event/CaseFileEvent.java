/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

/**
 * Event caused by a transition on a CaseFileItem
 */
@Manifest
public class CaseFileEvent extends CaseInstanceEvent {
    private final static Logger logger = LoggerFactory.getLogger(CaseFileEvent.class);

    private final String name;
    private final CaseFileItemTransition transition;
    private final Value<?> value;
    private final String path;
    private final State state;
    private final int index;

    public enum Fields {
        name, transition, value, path, state, index
    }

    public CaseFileEvent(Case caseInstance, String name, CaseFileItemTransition transition, Value<?> newValue, Path path, State currentState, int index) {
        super(caseInstance);
        this.name = name;
        this.transition = transition;
        this.value = newValue;
        this.path = path.toString();
        this.state = currentState;
        this.index = index;
    }

    public CaseFileEvent(ValueMap json) {
        super(json);
        this.name = json.raw(Fields.name);
        this.transition = json.getEnum(Fields.transition, CaseFileItemTransition.class);
        this.value = json.get(Fields.value.toString());
        this.path = json.raw(Fields.path);
        this.state = json.getEnum(Fields.state, State.class);
        this.index = json.rawInt(Fields.index.toString());
    }

    @Override
    public String toString() {
        return path + "->" + transition + " with value\n" + getValue() + "\n";
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
     * @return
     */
    public State getState() {
        return state;
    }

    /**
     * Returns the index of the case file item within it's parent (or -1 if it is not an iterable case file item)
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the name of the case file item
     *
     * @return
     */
    public String getCaseFileItemName() {
        return name;
    }

    /**
     * Returns the new value of the case file item.
     *
     * @return
     */
    public Value<?> getValue() {
        return value;
    }

    /**
     * Return the case file item's path through which the change was made (e.g., Order/Line)
     *
     * @return
     */
    public String getPath() {
        return path;
    }

    @Override
    public void recover(Case caseInstance) {
        try {
            // Have to recover it this way in order to overcome fact that Path.definition is not serializable
            Path recoveredPath = new Path(path, caseInstance);

            // Resolve the path on the case file
            CaseFileItem caseFileItem = caseInstance.getCaseFile().getItem(recoveredPath);

            caseFileItem.recover(this);
        } catch (InvalidPathException shouldNotHappen) {
            logger.error("Could not recover path on case instance?!", shouldNotHappen);
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.transition, transition);
        writeField(generator, Fields.value, value);
        writeField(generator, Fields.path, path);
        writeField(generator, Fields.state, state);
        generator.writeNumberField(Fields.index.toString(), index);
    }
}
