/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.engine.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.engine.cmmn.expression.spel.api.APIObject;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.team.Team;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class CaseTeamAPI extends BaseTeamAPI {
    private final Map<String, CaseRoleAPI> rolesByName = new HashMap<>();
    private final List<MemberAPI> members = new ArrayList<>();
    private final RoleAPI roleAPI;

    public CaseTeamAPI(Team team) {
        super(team);
        this.roleAPI = new RoleAPI(getActor());
        addPropertyReader("members", this::getMembers);
        addPropertyReader("users", this::getUsers);
        addPropertyReader("groups", this::getGroups);
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

    private Collection<MemberAPI> getGroups() {
        return team.getGroups().stream().map(this::wrap).collect(Collectors.toList());
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
