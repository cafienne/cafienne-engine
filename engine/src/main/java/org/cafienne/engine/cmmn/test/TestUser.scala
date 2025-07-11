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

package org.cafienne.engine.cmmn.test

import org.cafienne.actormodel.identity.{CaseUserIdentity, Origin}
import org.cafienne.engine.cmmn.actorapi.command.team.CaseTeamUser

class TestUser(userId: String, userRoles: Array[String]) extends CaseUserIdentity {
  override val id: String = userId
  override val origin: Origin = Origin.Tenant
  override val tenantRoles: Set[String] = userRoles.toSet

  def asCaseMember: CaseTeamUser = CaseTeamUser.from(id, origin, tenantRoles)

  def asCaseOwner: CaseTeamUser = CaseTeamUser.from(id, origin, tenantRoles, isOwner = true)
}