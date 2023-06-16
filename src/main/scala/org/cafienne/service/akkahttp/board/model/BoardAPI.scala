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

package org.cafienne.service.akkahttp.board.model

import org.cafienne.infrastructure.akkahttp.EntityReader.{EntityReader, entityReader}

object BoardAPI {
  //SEE BoardQueryProtocol
  implicit val boardRequestReader: EntityReader[BoardRequestDetails] = entityReader[BoardRequestDetails]
  implicit val boardSummaryResponseReader: EntityReader[BoardSummaryResponse] = entityReader[BoardSummaryResponse]
  implicit val columnRequestDetailsReader: EntityReader[ColumnRequestDetails] = entityReader[ColumnRequestDetails]
  implicit val teamMemberDetailsReader: EntityReader[TeamMemberDetails] = entityReader[TeamMemberDetails]

  case class BoardRequestDetails(id: String, title: String)

  case class BoardSummaryResponse(id: String, title: String)

  case class ColumnRequestDetails(id: String, title: String)

  case class TeamMemberDetails(userId: String, roles: Set[String])
}
