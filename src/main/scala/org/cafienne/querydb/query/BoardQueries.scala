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

package org.cafienne.querydb.query

import org.cafienne.actormodel.identity.{BoardUser, UserIdentity}
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.querydb.record.{BoardRecord, TaskRecord}

import scala.concurrent.Future

trait BoardQueries {

  def getBoards(user: UserIdentity): Future[Seq[BoardRecord]]

  def getBoardTasks(user: BoardUser): Future[Seq[TaskRecord]]

  def getBoardUser(): Future[BoardUser]
}

class BoardQueriesImpl extends BoardQueries
  with BaseQueryImpl {

  import dbConfig.profile.api._

  override def getBoards(user: UserIdentity): Future[Seq[BoardRecord]] = {
    val query = TableQuery[BoardTable]
      .join(TableQuery[ConsentGroupMemberTable]
        .filter(_.userId === user.id))
      .on(_.team === _.group)
      .map(_._1)

    db.run(query.distinct.result)
  }

  override def getBoardTasks(user: BoardUser): Future[Seq[TaskRecord]] = {
    val boardId = user.boardId

    val query = TableQuery[TaskTable]
      .filter(task =>
        TableQuery[CaseBusinessIdentifierTable]
          .filter(_.caseInstanceId === task.caseInstanceId)
          .filter(_.name === BoardDefinition.BOARD_IDENTIFIER)
          .filter(_.value === boardId).exists)
      .filter(task =>
        // Apply the filters: _1 is the case instance id, _2 is the case role
        TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id)
          .join(TableQuery[CaseInstanceTeamGroupTable])
          .on((group, membership) => group.group === membership.groupId)
          .map(_._2.caseInstanceId).filter(task.caseInstanceId === _).exists)

    db.run(query.distinct.result)
  }

  override def getBoardUser(): Future[BoardUser] = {
    ???
  }
}
