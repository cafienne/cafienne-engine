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

package org.cafienne.storage.querydb

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

class TenantStorage extends QueryDBStorage {

  import dbConfig.profile.api._

  def deleteTenant(tenant: String): Unit = {
    addStatement(TableQuery[UserRoleTable].filter(_.tenant === tenant).delete)
    addStatement(TableQuery[TenantTable].filter(_.name === tenant).delete)
    commit()
  }

  def readCases(tenant: String): Seq[String] = {
    val query = TableQuery[CaseInstanceTable].filter(_.tenant === tenant).filter(_.parentCaseId === "").map(_.id).distinct
    Await.result(db.run(query.result), 21.seconds)
  }

  def readGroups(tenant: String): Seq[String] = {
    val query = TableQuery[ConsentGroupTable].filter(_.tenant === tenant).map(_.id).distinct
    Await.result(db.run(query.result), 21.seconds)
  }
}
