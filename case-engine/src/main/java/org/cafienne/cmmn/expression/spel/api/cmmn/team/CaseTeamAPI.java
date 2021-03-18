package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;

/**
 */
public class CaseTeamAPI extends APIObject<Case> {
    public CaseTeamAPI(Team team) {
        super(team.getCaseInstance());
        addPropertyReader("members", () -> team.getMembers());
    }
}
