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
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

object BoardAPI {
  //SEE BoardQueryProtocol
  implicit val boardRequestReader: EntityReader[BoardRequestDetails] = entityReader[BoardRequestDetails]
  implicit val boardSummaryResponseReader: EntityReader[BoardSummaryResponse] = entityReader[BoardSummaryResponse]
  implicit val columnRequestDetailsReader: EntityReader[ColumnRequestDetails] = entityReader[ColumnRequestDetails]
  implicit val teamMemberDetailsReader: EntityReader[TeamMemberDetails] = entityReader[TeamMemberDetails]

  case class BoardRequestDetails(id: String, title: String)

  case class BoardSummaryResponse(id: String, title: String)

  case class ColumnRequestDetails(id: String, title: String)

  case class TeamMemberDetails(userId: String, name: Option[String], roles: Set[String]) extends CafienneJson {
    override def toValue: Value[_] = new ValueMap(Fields.userId, userId, Fields.name, name, Fields.roles, roles)
  }

  //TODO Column is a duplicate also found in BoardQueryProtocol
  final case class Column(
                     id: String,
                     position: Int,
                     title: Option[String],
                     role: Option[String],
                     tasks: Seq[Task]
                   ) extends CafienneJson {
    override def toValue: Value[_] = new ValueMap(Fields.id, id, "position", position, Fields.title, title, Fields.role, role, "tasks", tasks)
  }

  final case class Task(
                     id: String,
                     subject: Option[String],
                     description: Option[String],
                     position: Int,
                     caseInstanceId: String,
                     claimedBy: Option[String]
                   ) extends CafienneJson {
    override def toValue: Value[_] = new ValueMap(Fields.id, id, Fields.subject, subject, Fields.description, description, "position", position, Fields.caseInstanceId, caseInstanceId, "claimedBy", claimedBy)
  }

  case class BoardResponse(id: String, title: Option[String], team: Seq[TeamMemberDetails], columns: Seq[Column]) extends CafienneJson {
    override def toValue: Value[_] = new ValueMap(Fields.id, id, Fields.title, title, Fields.team, team, Fields.columns, columns)
  }
}
