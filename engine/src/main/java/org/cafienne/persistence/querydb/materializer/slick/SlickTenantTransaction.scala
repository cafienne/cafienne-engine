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

import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.persistence.querydb.materializer.tenant.TenantStorageTransaction
import org.cafienne.persistence.querydb.record.{TenantRecord, UserRoleKey, UserRoleRecord}

class SlickTenantTransaction extends SlickQueryDBTransaction with TenantStorageTransaction {

  import dbConfig.profile.api._

  override def upsert(record: TenantRecord): Unit = addStatement(TableQuery[TenantTable].insertOrUpdate(record))

  override def upsert(record: UserRoleRecord): Unit = addStatement(TableQuery[UserRoleTable].insertOrUpdate(record))

  override def delete(record: UserRoleRecord): Unit = {
    addStatement(TableQuery[UserRoleTable]
      .filter(_.userId === record.userId)
      .filter(_.tenant === record.tenant)
      .filter(_.role_name === record.role_name)
      .delete)
  }

  override def deleteTenantUser(user: TenantUser): Unit = {
    addStatement(TableQuery[UserRoleTable].filter(userRoleRecord => userRoleRecord.userId === user.id && userRoleRecord.tenant === user.tenant).delete)
  }

  override def getUserRole(key: UserRoleKey): Option[UserRoleRecord] = {
    runSync(TableQuery[UserRoleTable].filter(_.userId === key.userId).filter(_.tenant === key.tenant).filter(_.role_name === key.role_name).result.headOption)
  }

  override def updateTenantUserInformation(tenant: String, info: Seq[NewUserInformation], offset: OffsetRecord): Unit = {
    // Update logic has some complexity when the multiple old user id's are mapped to the same new user id
    //  In that case, duplicate key insertion may occur with the earlier approach that is done through 'simpleUpdate' below.
    val simpleUpdate = info.filter(u => u.newUserId != u.existingUserId).map(user => {
      (for {c <- TableQuery[UserRoleTable].filter(r => r.userId === user.existingUserId && r.tenant === tenant)} yield c.userId).update(user.newUserId)
    })

    val infoPerNewUserId: Set[(String, Set[String])] = convertUserUpdate(info)
    val hasNoDuplicates = !infoPerNewUserId.exists(update => update._2.size <= 1)

    // If there are no updates on different user id's to one new user id, then the update is simple

    val statements = if (hasNoDuplicates) {
      simpleUpdate
    } else {
      val oldUserIds = info.map(_.existingUserId).toSet
      val allOldUsers = TableQuery[UserRoleTable].filter(r => r.tenant === tenant && r.userId.inSet(oldUserIds))
      val records = runSync(allOldUsers.result)
      if (records.nonEmpty) {
        val deleteOldUsers = allOldUsers.delete
        val insertNewUsers = {
          infoPerNewUserId.flatMap(member => {
            val newMemberId = member._1

            val updatableRecords = records.filter(record => member._2.contains(record.userId))
            val userRecords = updatableRecords.filter(_.role_name.isBlank)
            val roleRecords = updatableRecords.filterNot(_.role_name.isBlank)

            // First user's name and email are taken as the "truth"; note: if there is no user record, a blank name and email are given
            val name = userRecords.headOption.fold("")(_.name)
            val email = userRecords.headOption.fold("")(_.email)
            val isOwner = userRecords.filter(_.enabled).filter(_.isOwner).toSet.nonEmpty
            val accountIsEnabled = userRecords.filter(_.enabled).toSet.nonEmpty

            val distinctActiveRoles = roleRecords.filter(_.enabled).map(_.role_name).toSet
            val distinctInactiveRoles = roleRecords.filterNot(_.enabled).filterNot(m => distinctActiveRoles.contains(m.role_name)).map(_.role_name).toSet

            val newUsersAndRoles: Seq[UserRoleRecord] = {
              // New user record
              Seq(UserRoleRecord(newMemberId, tenant, role_name = "", name = name, email = email, isOwner = isOwner, enabled = accountIsEnabled)) ++
                // Active roles of the user
                distinctActiveRoles.map(roleName => UserRoleRecord(newMemberId, tenant, role_name = roleName, name = "", email = "", isOwner = false, enabled = true)) ++
                // Inactive roles of the user
                distinctInactiveRoles.map(roleName => UserRoleRecord(newMemberId, tenant, role_name = roleName, name = "", email = "", isOwner = false, enabled = false))
            }
            newUsersAndRoles.map(record => TableQuery[UserRoleTable].insertOrUpdate(record))
          })
        }
        Seq(deleteOldUsers) ++ insertNewUsers
      } else {
        // If there are no records, then we can simply use the old statement. Actually - do we even need to do anything?
        simpleUpdate
      }
    }
    runSync(DBIO.sequence(statements ++ addOffsetRecord(offset)).transactionally)
  }
}
