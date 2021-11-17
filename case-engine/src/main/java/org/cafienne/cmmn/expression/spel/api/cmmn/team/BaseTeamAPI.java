package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;

class BaseTeamAPI extends APIObject<Case> {
    protected final Team team;

    protected BaseTeamAPI(Team team) {
        super(team.getCaseInstance());
        this.team = team;
    }

    protected MemberAPI wrap(CaseTeamMember member) {
        return new MemberAPI(team, member);
    }
}
