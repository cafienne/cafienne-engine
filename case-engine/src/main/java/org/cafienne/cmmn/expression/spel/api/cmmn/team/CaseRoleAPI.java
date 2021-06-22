package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;

import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class CaseRoleAPI extends APIObject<Case> {
    private final Team team;
    private final CaseRoleDefinition role;
    private final List<MemberAPI> members;
    private final List<MemberAPI> users;

    public CaseRoleAPI(Team team, CaseRoleDefinition role) {
        super(team.getCaseInstance());
        this.team = team;
        this.role = role;
        this.members = team.getMembers().stream().filter(member -> member.getRoles().contains(role)).map(MemberAPI::new).collect(Collectors.toList());
        this.users = team.getMembers().stream().filter(member -> member.getRoles().contains(role) && member.isUser()).map(MemberAPI::new).collect(Collectors.toList());
        addPropertyReader("members", () -> this.members);
        addPropertyReader("member", () -> this.members.stream().findFirst().orElse(null));
        addPropertyReader("users", () -> this.users);
        addPropertyReader("user", () -> this.users.stream().findFirst().orElse(null));
    }
}
