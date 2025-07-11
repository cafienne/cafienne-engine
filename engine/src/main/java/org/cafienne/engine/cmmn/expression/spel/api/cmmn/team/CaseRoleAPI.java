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

import org.cafienne.engine.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.engine.cmmn.instance.team.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 */
public class CaseRoleAPI extends BaseTeamAPI {
    private final CaseRoleDefinition role;
    private final Collection<MemberAPI> members;
    private final Collection<MemberAPI> users;
    private final Collection<MemberAPI> tenantRoles;
    private final Collection<MemberAPI> groups;

    public CaseRoleAPI(Team team, CaseRoleDefinition role) {
        super(team);
        this.role = role;
        this.users = team.getUsers().stream().map(this::wrap).collect(Collectors.toList());
        this.tenantRoles = team.getTenantRoles().stream().map(this::wrap).collect(Collectors.toList());
        this.groups = team.getGroups().stream().map(this::wrap).collect(Collectors.toList());

        this.members = new ArrayList<>();
        members.addAll(users);
        members.addAll(tenantRoles);
        members.addAll(groups);

        addPropertyReader("members", () -> this.members);
        addPropertyReader("member", () -> this.members.stream().findFirst().orElse(null));
        addPropertyReader("users", () -> this.users);
        addPropertyReader("user", () -> this.users.stream().findFirst().orElse(null));
        addPropertyReader("tenantRoles", () -> this.tenantRoles);
        addPropertyReader("tenantRole", () -> this.tenantRoles.stream().findFirst().orElse(null));
        addPropertyReader("groups", () -> this.groups);
        addPropertyReader("group", () -> this.groups.stream().findFirst().orElse(null));
    }
}
