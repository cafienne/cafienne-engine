/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class CaseInstanceEvent extends ModelEvent<Case> {
    public static final String TAG = "cafienne:case";
    private final static Logger logger = LoggerFactory.getLogger(CaseInstanceEvent.class);

    private transient final Case caseInstance;
    private transient CaseInstanceEvent source;
    private transient List<String> log = new ArrayList<>();
    private transient List<CaseInstanceEvent> childEvents = new ArrayList<>();

    protected CaseInstanceEvent(Case caseInstance) {
        super(caseInstance);
        this.caseInstance = caseInstance; // transient
    }

    protected CaseInstanceEvent(ValueMap json) {
        super(json);
        this.caseInstance = null; // transient
    }

    public final String getCaseInstanceId() {
        return this.getActorId();
    }

    final void setParent(CaseInstanceEvent parent) {
        if (parent != null) {
            source = parent;
            parent.childEvents.add(this);
        } else {
            logCounter = 0;
        }
    }

    final String getIndent() {
        if (source == null) {
            return "";
        } else {
            return source.getIndent() + "\t";
        }
    }

    private static int logCounter = 0;

    final void addLogMessage(String message) {
        message = getIndent() + "[" + (logCounter++) + "-" + getClass().getSimpleName().charAt(0) + "]:" + message;
        logger.debug(message);
        log.add(message);
        if (source != null) {
            // source.addMessage(message);
        }
    }

    public void finished() {
        if (caseInstance != null) {
            caseInstance.setCurrentEvent(source);
        }
    }

    protected void writeCaseInstanceEvent(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
    }
}
