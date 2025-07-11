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

import org.cafienne.engine.cmmn.instance.Case;

/**
 * Wrapper around the current user's case team membership, including the JWT token of the user.
 */
public class CurrentMemberAPI extends MemberAPI {
    /**
     * Create a member api for the current user - this includes the option to use the token of the currently active user,
     * but not possible to the token of other case team members.
     * @param caseInstance
     */
    public CurrentMemberAPI(Case caseInstance) {
        super(caseInstance.getCaseTeam(), caseInstance.getCurrentTeamMember());
        addPropertyReader("token", this::getToken);
    }

    private String getToken() {
        return actor.caseSystem.identityRegistration().getUserToken(actor.getCurrentUser());
    }
}
