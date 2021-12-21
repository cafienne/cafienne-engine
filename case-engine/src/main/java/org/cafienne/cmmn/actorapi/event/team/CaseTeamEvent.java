package org.cafienne.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.CaseBaseEvent;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in team events to do initial filtering.
 */
public abstract class CaseTeamEvent extends CaseBaseEvent {
    protected CaseTeamEvent(Team team) {
        super(team.getCaseInstance());
    }

    protected CaseTeamEvent(ValueMap json) {
        super(json);
    }

    protected void writeCaseTeamEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
    }

    @Override
    public void updateState(Case actor) {
        updateState(actor.getCaseTeam());
    }

    protected abstract void updateState(Team team);
}
