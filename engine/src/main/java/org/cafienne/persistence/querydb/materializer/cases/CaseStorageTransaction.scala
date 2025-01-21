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

package org.cafienne.persistence.querydb.materializer.cases

import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.persistence.querydb.materializer.QueryDBTransaction
import org.cafienne.persistence.querydb.materializer.cases.team.CaseTeamMemberKey
import org.cafienne.persistence.querydb.record._

trait CaseStorageTransaction extends QueryDBTransaction {
  def upsert(record: CaseRecord): Unit

  def upsert(record: CaseDefinitionRecord): Unit

  def upsert(record: TaskRecord): Unit

  def upsert(record: PlanItemRecord): Unit

  def upsert(record: CaseFileRecord): Unit

  def upsert(record: CaseBusinessIdentifierRecord): Unit

  def upsert(record: CaseRoleRecord): Unit

  def upsert(record: CaseTeamUserRecord): Unit

  def upsert(record: CaseTeamTenantRoleRecord): Unit

  def upsert(record: CaseTeamGroupRecord): Unit

  def delete(record: CaseTeamUserRecord): Unit

  def delete(record: CaseTeamTenantRoleRecord): Unit

  def delete(record: CaseTeamGroupRecord): Unit

  def deleteTaskRecord(taskId: String): Unit

  def deleteCaseTeamMember(key: CaseTeamMemberKey): Unit

  def deletePlanItemRecord(planItemId: String): Unit

  def removeCaseRoles(caseInstanceId: String): Unit

  def getPlanItem(planItemId: String): Option[PlanItemRecord]

  def getCaseFile(caseInstanceId: String): Option[CaseFileRecord]

  def getCaseInstance(caseInstanceId: String): Option[CaseRecord]

  def getTask(taskId: String): Option[TaskRecord]

  def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offset: OffsetRecord): Unit
}
