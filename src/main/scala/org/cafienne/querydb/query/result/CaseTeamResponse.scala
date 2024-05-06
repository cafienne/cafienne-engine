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

package org.cafienne.querydb.query.result

import org.cafienne.cmmn.actorapi.command.team.CaseTeam
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value}

final case class CaseTeamResponse(team: CaseTeam, caseRoles: Seq[String] = Seq(),
                                  unassignedRoles: Seq[String] = Seq()) extends CafienneJson {
  override def toValue: Value[_] = team.toValue.asMap.plus(Fields.caseRoles, caseRoles, Fields.unassignedRoles, unassignedRoles)
}
