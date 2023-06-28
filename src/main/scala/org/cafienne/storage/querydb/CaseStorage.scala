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

import akka.Done

import scala.concurrent.Future

class CaseStorage extends QueryDBStorage {

  import dbConfig.profile.api._

  def deleteCase(caseId: String): Future[Done] = {
    addStatement(TableQuery[CaseInstanceDefinitionTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[PlanItemTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[CaseFileTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[CaseBusinessIdentifierTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[CaseInstanceRoleTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[CaseInstanceTeamUserTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[TaskTable].filter(_.caseInstanceId === caseId).delete)
    addStatement(TableQuery[CaseInstanceTable].filter(_.id === caseId).delete)
    commit()
  }
}
