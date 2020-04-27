package org.cafienne.cmmn.akka.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in team events to do initial filtering.
 */
public abstract class CaseTeamEvent extends CaseEvent {
    protected enum Fields {
        userId, roles
    }

    protected CaseTeamEvent(Case caseInstance) {
        super(caseInstance);
    }

    protected CaseTeamEvent(ValueMap json) {
        super(json);
    }

    protected void writeCaseTeamEvent(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
    }
}
