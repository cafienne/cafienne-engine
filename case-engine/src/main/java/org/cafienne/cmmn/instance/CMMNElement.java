/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.instance.debug.DebugAppender;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;

public class CMMNElement<T extends CMMNElementDefinition> {
    private final Case caseInstance;
    private final T definition;

    protected CMMNElement() {
        // NOTE: this constructor is added to overcome serialization issues for task parameters. To be reviewed when we review the whole serialization structure again.
        this.caseInstance = null;
        this.definition = null;
    }

    protected CMMNElement(Case caseInstance, T definition) {
        this.caseInstance = caseInstance;
        this.definition = definition;
    }

    protected CMMNElement(CMMNElement<?> parentElement, T definition) {
        this.caseInstance = parentElement.caseInstance;
        this.definition = definition;
    }

    protected <T extends DebugEvent> void addDebugInfo(Class<T> eventClass, DebugAppender<T> appender) {
        getCaseInstance().addDebugInfo(eventClass, appender);
    }

    protected void addDebugInfo(DebugStringAppender appender) {
        getCaseInstance().addDebugInfo(appender);
    }

    public T getDefinition() {
        return definition;
    }

    public Case getCaseInstance() {
        return caseInstance;
    }

    public String toString() {
        // TODO: make this print the name
        return super.toString();
    }

}
