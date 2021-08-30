package org.cafienne.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in team events to do initial filtering.
 */
public abstract class CaseTeamEvent extends CaseEvent {
    protected CaseTeamEvent(Case caseInstance) {
        super(caseInstance);
    }

    protected CaseTeamEvent(ValueMap json) {
        super(json);
    }

    protected void writeCaseTeamEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
    }
}
