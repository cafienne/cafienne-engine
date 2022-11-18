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

package org.cafienne.cmmn.actorapi.response

/**
  * Case classes for response payloads of CaseCommands
  */
object CaseResponseModels {
  case class StartCaseResponse(caseInstanceId: String, name: String)

  case class DiscretionaryItem(name: String, definitionId: String, `type`: String, parentName: String, parentType: String, parentId: String)

  case class DiscretionaryItemsList(caseInstanceId: String, discretionaryItems: Seq[DiscretionaryItem])

  case class PlannedDiscretionaryItem(planItemId: String)
}
