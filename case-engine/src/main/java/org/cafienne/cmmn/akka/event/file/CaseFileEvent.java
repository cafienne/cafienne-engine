/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.sentry.StandardEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Event caused by a transition on a CaseFileItem
 */
@Manifest
public class CaseFileEvent extends CaseEvent implements StandardEvent<CaseFileItemTransition> {
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

    public CaseFileEvent(Case caseInstance, String name, State newState, CaseFileItemTransition transition, Value<?> newValue, Path path, int index) {
        super(caseInstance);
        this.name = name;
        this.transition = transition;
        this.value = newValue;
        this.path = path.toString();
        this.state = newState;
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

    private transient CaseFileItem caseFileItem;

    @Override
    public void updateState(Case caseInstance) {
        try {
            // Have to recover it this way in order to overcome fact that Path.definition is not serializable
            Path recoveredPath = new Path(path, caseInstance);

            // Resolve the path on the case file
            caseFileItem = caseInstance.getCaseFile().getItem(recoveredPath);
            caseFileItem.updateState(this);
        } catch (InvalidPathException shouldNotHappen) {
            logger.error("Could not recover path on case instance?!", shouldNotHappen);
        }
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
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.transition, transition);
        writeField(generator, Fields.value, value);
        writeField(generator, Fields.path, path);
        writeField(generator, Fields.state, state);
        generator.writeNumberField(Fields.index.toString(), index);
    }
}
