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

package org.cafienne.querydb.record

import org.cafienne.humantask.instance.TaskState
import org.cafienne.json._

import java.time.Instant

final case class TaskRecord(id: String,
                            caseInstanceId: String,
                            tenant: String,
                            taskName: String = "",
                            taskState: String = "",
                            role: String = "",
                            assignee: String = "",
                            owner: String = "",
                            dueDate: Option[Instant] = None,
                            createdOn: Instant,
                            createdBy: String = "",
                            lastModified: Instant,
                            modifiedBy: String = "",
                            input: String = "",
                            output: String = "",
                            taskModel: String = ""
                     ) extends CafienneJson {

  def getJSON(value: String): Value[_] = {
    if (value == "" || value == null) new ValueMap else {
      try {
        JSONReader.parse(value)
      } catch {
        case _: Throwable => new StringValue(value)
      }
    }
  }

  def isActive(): Boolean = {
    TaskState.valueOf(taskState) match {
      case TaskState.Unassigned => true
      case TaskState.Assigned => true
      case TaskState.Delegated => true
      case TaskState.Suspended => true
      case other => false
    }
  }

  override def toValue: Value[_] = {
    val v = new ValueMap
    v.plus("id", id)
    v.plus("taskName", taskName)
    v.plus("taskState", taskState)
    v.plus("assignee", assignee)
    v.plus("owner", owner)
    v.plus("tenant", tenant)
    v.plus("caseInstanceId", caseInstanceId)
    v.plus("role", role)
    v.plus("lastModified", lastModified)
    v.plus("modifiedBy", modifiedBy)
    v.plus("dueDate", dueDate.getOrElse(""))
    v.plus("createdOn", createdOn)
    v.plus("createdBy", createdBy)
    v.plus("input", getJSON(input))
    v.plus("output", getJSON(output))
    v.plus("taskModel", getJSON(taskModel))
    v
  }
}
