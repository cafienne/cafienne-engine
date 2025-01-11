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

package org.cafienne.querydb.materializer.cases.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.Origin
import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent
import org.cafienne.cmmn.actorapi.event.team.deprecated.member._
import org.cafienne.cmmn.actorapi.event.team.deprecated.user.{DeprecatedCaseTeamUserEvent, TeamMemberAdded}
import org.cafienne.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.querydb.record.{CaseTeamTenantRoleRecord, CaseTeamUserRecord}

class DeprecatedCaseTeamEventProjection(dBTransaction: CaseStorageTransaction) extends LazyLogging {
  private val upsertableUsers = scala.collection.mutable.HashMap[(String, String, String), CaseTeamUserRecord]()
  private val deletableUsers = scala.collection.mutable.HashMap[(String, String, String), CaseTeamUserRecord]()
  private val upsertableTenantRoles = scala.collection.mutable.HashMap[(String, String, String, String), CaseTeamTenantRoleRecord]()
  private val deletableTenantRoles = scala.collection.mutable.HashMap[(String, String, String, String), CaseTeamTenantRoleRecord]()

  def handleDeprecatedCaseTeamEvent(event: DeprecatedCaseTeamEvent): Unit = {
    event match {
      case event: DeprecatedCaseTeamUserEvent => handleDeprecatedCaseTeamUserEvent(event)
      case other => handleCaseTeamMemberEvent(event)
    }
  }

  private def handleDeprecatedCaseTeamUserEvent(event: DeprecatedCaseTeamUserEvent): Unit = {
    // Deprecated case team events have all member roles in them; these members are always of type user; all those users become owner and active;
    import scala.jdk.CollectionConverters._
    // We need to add the empty role (if not yet there),
    //  in order to have member table also populated when a member has no roles but still is part of the team
    val roles = (Seq("") ++ event.getRoles.asScala).toSet

    // For each role create a corresponding record.
    val records: Set[((String, String, String), CaseTeamUserRecord)] = roles.map(role => {
      val key = (event.getActorId, event.getUserId, role)
      val record = CaseTeamUserRecord(caseInstanceId = event.getActorId, tenant = event.tenant, userId = event.getUserId, origin = Origin.Tenant.toString, caseRole = role, isOwner = true)
      (key, record)
    })

    // Now determine whether to add or to delete the records
    if (event.isInstanceOf[TeamMemberAdded]) {
      records.foreach(record => addUser(record._1, record._2))
    } else {
      records.foreach(record => removeUser(record._1, record._2))
    }
  }

  private def addUser(key: (String, String, String), user: CaseTeamUserRecord): Unit = {
    upsertableUsers.put(key, user)
    deletableUsers.remove(key)
  }

  private def removeUser(key: (String, String, String), user: CaseTeamUserRecord): Unit = {
    deletableUsers.put(key, user)
    upsertableUsers.remove(key)
  }

  private def handleCaseTeamMemberEvent(event: DeprecatedCaseTeamEvent): Unit = {
    if (event.isTenantUser) {
      handleCaseTeamUserEvent(event)
    } else {
      handleCaseTeamTenantRoleEvent(event)
    }
  }

  private def handleCaseTeamTenantRoleEvent(event: DeprecatedCaseTeamEvent): Unit = {
    // We handle 2 types of event: either the old ones (which carried all info in one shot) or the new ones, which are more particular
    val key = (event.getActorId, event.tenant, event.memberId, event.roleName)
    // Make sure to update any existing versions of the record (especially if first a user is added and at the same time becomes owner this is necessary)
    //  We have seen situation with SQL Server where the order of the update actually did not make a user owner
    val tenantRole = upsertableTenantRoles.getOrElseUpdate(key, CaseTeamTenantRoleRecord(event.getActorId, tenant = event.tenant, tenantRole = event.memberId, caseRole = event.roleName, isOwner = false))
    event match {
      case _: TeamRoleFilled => addTenantRole(key, tenantRole)
      case _: TeamRoleCleared => removeTenantRole(key, tenantRole)
      case _: CaseOwnerAdded => addTenantRole(key, tenantRole.copy(isOwner = true))
      case _: CaseOwnerRemoved => addTenantRole(key, tenantRole.copy(isOwner = false))
      case _ => // Ignore other events; are there other events?
    }
  }

  private def addTenantRole(key: (String, String, String, String), user: CaseTeamTenantRoleRecord): Unit = {
    upsertableTenantRoles.put(key, user)
    deletableTenantRoles.remove(key)
  }

  private def removeTenantRole(key: (String, String, String, String), user: CaseTeamTenantRoleRecord): Unit = {
    deletableTenantRoles.put(key, user)
    upsertableTenantRoles.remove(key)
  }

  private def handleCaseTeamUserEvent(event: DeprecatedCaseTeamEvent): Unit = {
    // We handle 2 types of event: either the old ones (which carried all info in one shot) or the new ones, which are more particular
    val key = (event.getActorId, event.memberId, event.roleName)
    // Make sure to update any existing versions of the record (especially if first a user is added and at the same time becomes owner this is necessary)
    //  We have seen situation with SQL Server where the order of the update actually did not make a user owner
    val user = upsertableUsers.getOrElseUpdate(key, CaseTeamUserRecord(event.getActorId, tenant = event.tenant, caseRole = event.roleName, origin = Origin.Tenant.toString, userId = event.memberId, isOwner = false))
    event match {
      case _: TeamRoleFilled => addUser(key, user)
      case _: TeamRoleCleared => removeUser(key, user)
      case _: CaseOwnerAdded => addUser(key, user.copy(isOwner = true))
      case _: CaseOwnerRemoved => addUser(key, user.copy(isOwner = false))
      case _ => // Ignore other events; are there other events?
    }
  }

  def prepareCommit(): Unit = {
    this.upsertableUsers.values.foreach(dBTransaction.upsert)
    this.deletableUsers.values.foreach(dBTransaction.delete)
    this.upsertableTenantRoles.values.foreach(dBTransaction.upsert)
    this.deletableTenantRoles.values.foreach(dBTransaction.delete)
  }
}
