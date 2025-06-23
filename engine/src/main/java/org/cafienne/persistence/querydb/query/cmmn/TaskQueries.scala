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

package org.cafienne.persistence.querydb.query.cmmn

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.json.{CafienneJson, LongValue, Value, ValueMap}
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.cmmn.filter.TaskFilter
import org.cafienne.persistence.querydb.record.TaskRecord

import scala.concurrent.Future

case class TaskCount(claimed: Long, unclaimed: Long) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap("claimed", new LongValue(claimed), "unclaimed", new LongValue(unclaimed))
}

trait TaskQueries {

  def getTask(taskId: String, user: UserIdentity): Future[TaskRecord]

  def getTasksWithCaseName(caseName: String, tenant: Option[String], user: UserIdentity): Future[Seq[TaskRecord]]

  def getAllTasks(user: UserIdentity, filter: TaskFilter = TaskFilter(), area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[TaskRecord]]

  def getCountForUser(user: UserIdentity, tenant: Option[String]): Future[TaskCount]
}
