package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 */
public class CaseTeamAPI extends BaseTeamAPI {
    private final Map<String, CaseRoleAPI> rolesByName = new HashMap<>();
    private final RoleAPI roleAPI;

    public CaseTeamAPI(Team team) {
        super(team);
        this.roleAPI = new RoleAPI(getActor());
        addPropertyReader("members", this::getMembers);
        addPropertyReader("users", this::getUsers);
        addPropertyReader("tenantRoles", this::getTenantRoles);
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
            team.getDefinition().getCaseRoles().forEach(role -> {
                rolesByName.put(role.getName(), new CaseRoleAPI(team, role));
            });
        }
        return rolesByName;
    }

    private Collection<MemberAPI> getUsers() {
        return team.getUsers().stream().map(this::wrap).collect(Collectors.toList());
    }

    private Collection<MemberAPI> getTenantRoles() {
        return team.getTenantRoles().stream().map(this::wrap).collect(Collectors.toList());
    }

    private Collection<MemberAPI> getOwners() {
        return getMembers().stream().filter(MemberAPI::isOwner).collect(Collectors.toList());
    }

    private Collection<MemberAPI> getMembers() {
        return team.getMembers().stream().map(this::wrap).collect(Collectors.toList());
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
