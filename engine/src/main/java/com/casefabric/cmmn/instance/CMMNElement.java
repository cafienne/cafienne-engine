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

package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.definition.DefinitionElement;
import org.cafienne.cmmn.instance.debug.DebugInfoAppender;

public class CMMNElement<T extends DefinitionElement> {
    private final Case caseInstance;
    private T definition;
    private T previousDefinition;

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

    protected void addDebugInfo(DebugInfoAppender appender, Object... additionalInfo) {
        getCaseInstance().addDebugInfo(appender, additionalInfo);
    }

    public T getDefinition() {
        return definition;
    }

    public T getPreviousDefinition() {
        return previousDefinition;
    }

    public void migrateDefinition(T newDefinition, boolean skipLogic) {
        this.previousDefinition = this.definition;
        this.definition = newDefinition;
    }

    public Case getCaseInstance() {
        return caseInstance;
    }

    public String toString() {
        // TODO: make this print the name
        return super.toString();
    }

    protected <T extends CaseEvent> T addEvent(T event) {
        return getCaseInstance().addEvent(event);
    }
}
