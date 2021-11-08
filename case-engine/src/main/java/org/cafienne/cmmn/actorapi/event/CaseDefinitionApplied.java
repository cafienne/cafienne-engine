/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.definition.CaseDefinitionEvent;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class CaseDefinitionApplied extends CaseDefinitionEvent {
    public final CafienneVersion engineVersion;
    private final String parentCaseId;
    private final String rootCaseId;
    public final Instant createdOn;
    public final String createdBy;

    public CaseDefinitionApplied(Case caseInstance, String rootCaseId, String parentCaseId, CaseDefinition definition) {
        super(caseInstance, definition);
        this.createdOn = caseInstance.getTransactionTimestamp();
        this.createdBy = caseInstance.getCurrentUser().id();
        this.rootCaseId = rootCaseId;
        this.parentCaseId = parentCaseId;
        // Whenever a new case is started, a case definition is applied.
        //  So, at that moment we also store the engine version.
        //  TODO: perhaps better to distinguish CaseStarted or CaseCreated from CaseDefinitionApplied
        //   If so, then we can also suffice with storing root id and so in the CaseCreated, rather than in case definition applied. Same for engine version.
        this.engineVersion = Cafienne.version();
    }

    public CaseDefinitionApplied(ValueMap json) {
        super(json);
        this.createdOn = json.readInstant(Fields.createdOn);
        this.createdBy = json.readString(Fields.createdBy);
        this.rootCaseId = json.readString(Fields.rootActorId);
        this.parentCaseId = json.readString(Fields.parentActorId);
        this.engineVersion = json.readObject(Fields.engineVersion, CafienneVersion::new);
    }

    /**
     * Returns the identifier of the outer-most parent that started this case. E.g., if this event happened in a sub case,
     * then this will return the id of it's top most ancestor case starting it, and for which the subcase is a blocking task.
     * So, non-blocking sub case tasks are stand alone, and will not return the identifier of the parent case that started it.
     * Furthermore, if this case is itself a root-case, then root case id and case instance id will be the same.
     *
     * @return
     */
    public String getRootCaseId() {
        return rootCaseId;
    }

    /**
     * Returns the identifier of the case that started the subcase causing this event to happen, or null if there was no parent.
     * Note, this will also return the parent case id if this subcase was started in non-blocking mode (as opposed to getRootCaseId behavior)
     *
     * @return
     */
    public String getParentCaseId() {
        return parentCaseId;
    }

    @Override
    public String toString() {
        return "Case definition " + getCaseName();
    }

    /**
     * Returns the case definition that was applied to the case instance
     * @return
     */
    public CaseDefinition getDefinition() {
        return this.definition;
    }

    public void updateState(Case caseInstance) {
        caseInstance.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseDefinitionEvent(generator);
        writeField(generator, Fields.createdOn, createdOn);
        writeField(generator, Fields.createdBy, createdBy);
        writeField(generator, Fields.rootActorId, rootCaseId);
        writeField(generator, Fields.parentActorId, parentCaseId);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}
