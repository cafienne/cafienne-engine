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

package org.cafienne.persistence.querydb.materializer.slick

import org.cafienne.engine.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.engine.cmmn.instance.team.MemberType
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.persistence.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.persistence.querydb.materializer.cases.team.CaseTeamMemberKey
import org.cafienne.persistence.querydb.record._
import org.cafienne.persistence.querydb.schema.table.{CaseTables, TaskTables}

class SlickCaseTransaction(val writer: QueryDBWriter) extends SlickQueryDBTransaction
  with CaseTables
  with TaskTables
  with CaseStorageTransaction {

  import dbConfig.profile.api._

  override def upsert(record: CaseRecord): Unit = addStatement(TableQuery[CaseInstanceTable].insertOrUpdate(record))

  override def upsert(record: CaseDefinitionRecord): Unit = addStatement(TableQuery[CaseInstanceDefinitionTable].insertOrUpdate(record))

  override def upsert(record: TaskRecord): Unit = addStatement(TableQuery[TaskTable].insertOrUpdate(record))

  override def upsert(record: PlanItemRecord): Unit = addStatement(TableQuery[PlanItemTable].insertOrUpdate(record))

  override def upsert(record: CaseFileRecord): Unit = addStatement(TableQuery[CaseFileTable].insertOrUpdate(record))

  override def upsert(record: CaseBusinessIdentifierRecord): Unit = addStatement(TableQuery[CaseBusinessIdentifierTable].insertOrUpdate(record))

  override def upsert(record: CaseRoleRecord): Unit = addStatement(TableQuery[CaseInstanceRoleTable].insertOrUpdate(record))

  override def upsert(record: CaseTeamUserRecord): Unit = addStatement(TableQuery[CaseInstanceTeamUserTable].insertOrUpdate(record))

  override def upsert(record: CaseTeamTenantRoleRecord): Unit = addStatement(TableQuery[CaseInstanceTeamTenantRoleTable].insertOrUpdate(record))

  override def upsert(record: CaseTeamGroupRecord): Unit = addStatement(TableQuery[CaseInstanceTeamGroupTable].insertOrUpdate(record))

  override def delete(record: CaseTeamUserRecord): Unit = addStatement(
    TableQuery[CaseInstanceTeamUserTable]
      .filter(_.caseInstanceId === record.caseInstanceId)
      .filter(_.userId === record.userId)
      .filter(_.caseRole === record.caseRole)
      .delete)

  override def delete(record: CaseTeamTenantRoleRecord): Unit = addStatement(
    TableQuery[CaseInstanceTeamTenantRoleTable]
      .filter(_.caseInstanceId === record.caseInstanceId)
      .filter(_.tenant === record.tenant)
      .filter(_.tenantRole === record.tenantRole)
      .filter(_.caseRole === record.caseRole)
      .delete)

  override def delete(record: CaseTeamGroupRecord): Unit = addStatement(
    TableQuery[CaseInstanceTeamGroupTable]
      .filter(_.caseInstanceId === record.caseInstanceId)
      .filter(_.groupId === record.groupId)
      .filter(_.groupRole === record.groupRole)
      .filter(_.caseRole === record.caseRole)
      .delete)

  override def deleteTaskRecord(taskId: String): Unit = {
    addStatement(TableQuery[TaskTable].filter(_.id === taskId).delete)
  }

  override def deleteCaseTeamMember(key: CaseTeamMemberKey): Unit = {
    key.memberType match {
      case MemberType.User => addStatement(TableQuery[CaseInstanceTeamUserTable].filter(_.caseInstanceId === key.caseInstanceId).filter(_.userId === key.memberId).delete)
      case MemberType.Group => addStatement(TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === key.caseInstanceId).filter(_.groupId === key.memberId).delete)
      case MemberType.TenantRole => addStatement(TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === key.caseInstanceId).filter(_.tenantRole === key.memberId).delete)
    }
  }

  override def deletePlanItemRecord(planItemId: String): Unit = {
    addStatement(TableQuery[PlanItemTable].filter(_.id === planItemId).delete)
  }

  override def removeCaseRoles(caseInstanceId: String): Unit = {
    addStatement(TableQuery[CaseInstanceRoleTable].filter(_.caseInstanceId === caseInstanceId).delete)
  }

  override def getPlanItem(planItemId: String): Option[PlanItemRecord] = {
    runSync(TableQuery[PlanItemTable].filter(_.id === planItemId).result.headOption)
  }

  override def getCaseInstance(id: String): Option[CaseRecord] = {
    runSync(TableQuery[CaseInstanceTable].filter(_.id === id).result.headOption)
  }

  override def getCaseFile(caseInstanceId: String): Option[CaseFileRecord] = {
    runSync(TableQuery[CaseFileTable].filter(_.caseInstanceId === caseInstanceId).result.headOption)
  }

  override def getTask(taskId: String): Option[TaskRecord] = {
    runSync(TableQuery[TaskTable].filter(_.id === taskId).result.headOption)
  }

  override def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offset: OffsetRecord): Unit = {
    // When the user id is updated, then all records relating to the case instance need to be updated
    //  - Case itself on [createdBy, modifiedBy]
    //  - CaseDefinition on [modifiedBy]
    //  - PlanItem on [createdBy, modifiedBy]
    //  - PlanItemHistory on [modifiedBy]
    //  - Tasks on [createdBy, modifiedBy, assignee, owner]
    //  - CaseTeam members --> this has special logic, as multiple old id's may map to one new id.
    //    Updating this requires a database query, therefore this is isolated in a separate method returning a Future
    val updateQueries = info.map(user => {
      // Update 'createdBy' field in case instance table
      (for {cases <- TableQuery[CaseInstanceTable].filter(r => r.id === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in case instance table
      (for {cases <- TableQuery[CaseInstanceTable].filter(r => r.id === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in case instance definition table
      (for {cases <- TableQuery[CaseInstanceDefinitionTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'createdBy' field in planitem table
      (for {cases <- TableQuery[PlanItemTable].filter(r => r.caseInstanceId === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in planitem table
      (for {cases <- TableQuery[PlanItemTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'createdBy' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'assignee' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.assignee === user.existingUserId)} yield cases.assignee).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'owner' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.owner === user.existingUserId)} yield cases.owner).update(user.newUserId)
    }) ++ addOffsetRecord(offset)

    // Now retrieve the case team updates, combine it with the above statements and run it
    runSync(DBIO.sequence(updateQueries ++ constructCaseTeamUserIdUpdates(caseId, info)).transactionally)
  }

  private def constructCaseTeamUserIdUpdates(caseId: String, info: Seq[NewUserInformation]): Seq[DBIO[_]] = {
    val infoPerNewUserId: Set[(String, Set[String])] = convertUserUpdate(info)
    val hasNoDuplicates = !infoPerNewUserId.exists(update => update._2.size <= 1)

    // If there are no duplicates (i.e., no situations where multiple old user id's map to the same new id)
    //  then the update is simple.
    // Otherwise we need to analyze the current database information (see below)
    val simpleCaseTeamUpdate = info.map(user => {
      // Update 'memberId' field in team table
      val member = for {
        member <- TableQuery[CaseInstanceTeamUserTable].filter(r => r.caseInstanceId === caseId && r.userId === user.existingUserId)
      } yield member.userId
      member.update(user.newUserId)
    })

    if (hasNoDuplicates) {
      simpleCaseTeamUpdate
    } else {
      // Run a query on the old user id's, in order to find out what their role in the case team is
      //  - Subsequently delete all those old records, and replace them with a new set
      val oldUserIds = info.map(_.existingUserId).toSet
      val allOldMembers = TableQuery[CaseInstanceTeamUserTable].filter(r => r.caseInstanceId === caseId && r.userId.inSet(oldUserIds))
      val records = runSync(allOldMembers.result)
      if (records.nonEmpty) {
        val tenant = records.head.tenant
        val deleteOldMembers = allOldMembers.delete
        val insertNewMembers = {
          infoPerNewUserId.flatMap(member => {
            val newUserId = member._1
            val updatableRecords = records.filter(record => member._2.contains(record.userId))
            val userRecords = updatableRecords.filter(_.caseRole.isBlank)
            val roleRecords = updatableRecords.filterNot(_.caseRole.isBlank)

            val distinctActiveRoles = roleRecords.map(_.caseRole).toSet

            // The new member becomes case owner if one or more of the old members is also case owner
            val isCaseOwner = userRecords.filter(_.isOwner).toSet.nonEmpty

            val newMembers: Seq[CaseTeamUserRecord] = {
              Seq(CaseTeamUserRecord(caseInstanceId = caseId, tenant = tenant, userId = newUserId, origin = "tenant", caseRole = "", isOwner = isCaseOwner)) ++
                distinctActiveRoles.map(caseRole => CaseTeamUserRecord(caseInstanceId = caseId, tenant = tenant, userId = newUserId, origin = "tenant", caseRole = caseRole, isOwner = isCaseOwner))
            }
            newMembers.map(newMember => TableQuery[CaseInstanceTeamUserTable].insertOrUpdate(newMember))
          })
        }
        Seq(deleteOldMembers) ++ insertNewMembers
      } else {
        simpleCaseTeamUpdate
      }
    }
  }
}
