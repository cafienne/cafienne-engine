package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.instance.team.Team;

/**
 */
public class MemberAPI extends BaseTeamAPI {
    private final boolean isOwner;

    public MemberAPI(Team team, CaseTeamMember member) {
        super(team);
        this.isOwner = member.isOwner();
        addPropertyReader("isOwner", member::isOwner);
        addPropertyReader("id", member::memberId);
        addPropertyReader("type", member::memberType);
        addPropertyReader("roles", member::getCaseRoles);
    }

    boolean isOwner() {
        return isOwner;
    }
}
