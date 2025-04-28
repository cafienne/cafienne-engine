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

package org.cafienne.timerservice.persistence.jdbc

import org.cafienne.persistence.infrastructure.jdbc.SlickTableExtensions

import java.time.Instant

/**
  * final case class TimerServiceRecord(timerId: String,
  * caseInstanceId: String,
  * moment: Instant,
  * tenant: String,
  * user: String)
  */
trait TimerServiceTables extends SlickTableExtensions {
  import dbConfig.profile.api._

  // Schema for the "task" table:
  final class TimerServiceTable(tag: Tag) extends CafienneTable[TimerServiceRecord](tag, "timer") {

    def timerId = idColumn[String]("timer_id", O.PrimaryKey)

    def caseInstanceId = idColumn[String]("case_instance_id")

    def rootCaseId = column[String]("root_case_id", O.Default(""))

    def moment = column[Instant]("moment")

    def tenant = idColumn[String]("tenant")

    def user = column[String]("user", O.Default(""))

    // Various indices for optimizing getAllTasks queries
    def indexCaseInstanceId = oldStyleIndex(caseInstanceId)
    def indexTimerId = oldStyleIndex(timerId)
    def indexTenant = oldStyleIndex(tenant)
    def indexMoment = index(oldStyleIxName(moment), moment)
    def indexRootCaseId = oldStyleIndex(rootCaseId)

    def * = (timerId, caseInstanceId, rootCaseId, moment, tenant, user).mapTo[TimerServiceRecord]
  }
}
