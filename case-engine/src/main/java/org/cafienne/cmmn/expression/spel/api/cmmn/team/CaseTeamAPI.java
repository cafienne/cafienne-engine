package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;

/**
 */
public class CaseTeamAPI extends APIObject<Case> {
    private final Team team;

    public CaseTeamAPI(Team team) {
        super(team.getCaseInstance());
        this.team = team;
        addPropertyReader("members", () -> team.getMembers());
    }

    public String getMemberWithRole(String role) {
        warnDeprecation("getMemberWithRole(\""+role+"\")", "team.role[\""+role +"\"].member");
        return team.getMemberWithRole(role);
    }
}
