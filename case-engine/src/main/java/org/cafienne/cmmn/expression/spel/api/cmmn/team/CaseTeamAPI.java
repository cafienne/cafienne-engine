package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 */
public class CaseTeamAPI extends APIObject<Case> {
    private final Team team;
    private final Map<String, CaseRoleAPI> rolesByName = new HashMap();
    private final List<MemberAPI> members = new ArrayList();
    private final RoleAPI roleAPI;

    public CaseTeamAPI(Team team) {
        super(team.getCaseInstance());
        this.team = team;
        this.roleAPI = new RoleAPI(getActor());
        addPropertyReader("members", this::getMembers);
        addPropertyReader("users", this::getUsers);
        addPropertyReader("owners", this::getOwners);
        addPropertyReader("roles", () -> getRolesByName().values());
        addPropertyReader("role", this::getRoleAPI);
    }

    private RoleAPI getRoleAPI() {
        roleAPI.initialize(getRolesByName());
        return roleAPI;
    }

    private Map<String, CaseRoleAPI> getRolesByName() {
        if (rolesByName.isEmpty()) {
            team.getCaseInstance().getDefinition().getCaseRoles().forEach(role -> {
                rolesByName.put(role.getName(), new CaseRoleAPI(team, role));
            });
        }
        return rolesByName;
    }

    private List<MemberAPI> getOwners() {
        return getMembers().stream().filter(MemberAPI::isOwner).collect(Collectors.toList());
    }

    private List<MemberAPI> getUsers() {
        return getMembers().stream().filter(MemberAPI::isUser).collect(Collectors.toList());
    }

    private List<MemberAPI> getMembers() {
        if (members.isEmpty()) {
            team.getMembers().forEach(MemberAPI::new);
        }
        return members;
    }

    public String getMemberWithRole(String role) {
        warnDeprecation("getMemberWithRole(\""+role+"\")", "team.role[\""+role +"\"].member");
        return team.getMemberWithRole(role);
    }

    class RoleAPI extends APIObject<Case> {
        boolean initialized;

        protected RoleAPI(Case actor) {
            super(actor);
        }

        public void initialize(Map<String, CaseRoleAPI> rolesByName) {
            if (initialized) {
                return;
            }

            // Just add a property for each role. Makes this behave similar to a map
            rolesByName.forEach((role, api) -> addPropertyReader(role, () -> api));
        }
    }
}
