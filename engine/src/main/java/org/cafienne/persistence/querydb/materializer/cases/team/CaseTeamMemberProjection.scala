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

package org.cafienne.persistence.querydb.materializer.cases.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.command.team._
import org.cafienne.cmmn.actorapi.event.team.group.{CaseTeamGroupAdded, CaseTeamGroupChanged}
import org.cafienne.cmmn.actorapi.event.team.{CaseTeamMemberEvent, CaseTeamMemberRemoved}
import org.cafienne.cmmn.instance.team.MemberType
import org.cafienne.persistence.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.persistence.querydb.record.{CaseTeamGroupRecord, CaseTeamTenantRoleRecord, CaseTeamUserRecord}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class CaseTeamMemberProjection(dBTransaction: CaseStorageTransaction) extends LazyLogging {
  private val newCaseTeamUserRoles = ListBuffer[CaseTeamUserRecord]()
  private val removedCaseTeamUserRoles = ListBuffer[CaseTeamUserRecord]()
  private val newCaseTeamTenantRoleRoles = ListBuffer[CaseTeamTenantRoleRecord]()
  private val removedCaseTeamTenantRoleRoles = ListBuffer[CaseTeamTenantRoleRecord]()
  private val newCaseTeamGroupMappings = ListBuffer[CaseTeamGroupRecord]()
  private val removedGroupMappings = ListBuffer[CaseTeamGroupRecord]()
  private val deletedMembers = mutable.Set[CaseTeamMemberKey]()

  def handleEvent(event: CaseTeamMemberEvent[_]): Unit = {
    if (event.isInstanceOf[CaseTeamMemberRemoved[_]]) {
      val member: CaseTeamMember = event.member.asInstanceOf[CaseTeamMember]
      deletedMembers += CaseTeamMemberKey(caseInstanceId = event.getActorId, memberId = member.memberId, memberType = member.memberType)
    } else {
      event.member match {
        case user: CaseTeamUser => handleUserEvent(event, user)
        case role: CaseTeamTenantRole => handleTenantRoleEvent(event, role)
        case group: CaseTeamGroup => handleGroupEvent(event, group)
        case other => logger.warn(s"Unexpected type of case team member ${other.getClass.getSimpleName} while handling event of type ")
      }
    }
  }

  def handleUserEvent(event: CaseTeamMemberEvent[_], user: CaseTeamUser): Unit = {
    val caseInstanceId = event.getActorId
    val memberId: String = user.memberId
    val isOwner: Boolean = user.isOwner
    val caseRoles: Set[String] = user.caseRoles ++ Set("")
    val removedRoles: Set[String] = user.rolesRemoved
    val origin: String = user.origin.toString

    newCaseTeamUserRoles.addAll(caseRoles.map(caseRole => CaseTeamUserRecord(caseInstanceId = caseInstanceId, tenant = event.tenant, userId = memberId, origin = origin, caseRole = caseRole, isOwner = isOwner)))
    removedCaseTeamUserRoles.addAll(removedRoles.map(caseRole => CaseTeamUserRecord(caseInstanceId = caseInstanceId, tenant = event.tenant, userId = memberId, origin = origin, caseRole = caseRole, isOwner = isOwner)))
  }

  def handleTenantRoleEvent(event: CaseTeamMemberEvent[_], teamTenantRole: CaseTeamTenantRole): Unit = {
    val caseInstanceId = event.getActorId
    val memberId: String = teamTenantRole.memberId
    val isOwner: Boolean = teamTenantRole.isOwner
    val caseRoles: Set[String] = teamTenantRole.caseRoles ++ Set("")
    val removedRoles: Set[String] = teamTenantRole.rolesRemoved

    newCaseTeamTenantRoleRoles.addAll(caseRoles.map(caseRole => CaseTeamTenantRoleRecord(caseInstanceId = caseInstanceId, tenantRole = memberId, tenant = event.tenant, caseRole = caseRole, isOwner = isOwner)))
    removedCaseTeamTenantRoleRoles.addAll(removedRoles.map(caseRole => CaseTeamTenantRoleRecord(caseInstanceId = caseInstanceId, tenantRole = memberId, tenant = event.tenant, caseRole = caseRole, isOwner = isOwner)))
  }

  def handleGroupEvent(event: CaseTeamMemberEvent[_], group: CaseTeamGroup): Unit = {
    def asRecords(mapping: GroupRoleMapping): Set[CaseTeamGroupRecord] = (mapping.caseRoles ++ Set("")).map(asRecord(mapping, _))
    def asRecord(mapping: GroupRoleMapping, caseRole: String): CaseTeamGroupRecord = CaseTeamGroupRecord(caseInstanceId = event.getActorId, tenant = event.tenant, groupId = group.groupId, caseRole = caseRole, groupRole = mapping.groupRole, isOwner = mapping.isOwner)

    event match {
      // Add a record for each mapping
      case _: CaseTeamGroupAdded =>
        newCaseTeamGroupMappings.addAll(group.mappings.flatMap(asRecords))
      case _: CaseTeamGroupChanged =>
        // Upsert new case roles
        newCaseTeamGroupMappings.addAll(group.mappings.flatMap(asRecords))
        // Delete removed case roles
        removedGroupMappings.addAll(group.mappings.flatMap(mapping => mapping.rolesRemoved.map(asRecord(mapping, _))))
        // Delete removed groupRoles
        removedGroupMappings.addAll(group.removedMappings.flatMap(asRecords))
      case _ => // other events are not relevant
    }
  }

  def prepareCommit(): Unit = {
    // Update case team changes. Note: order matters (a bit). So first delete, and then add new info.
    removedCaseTeamUserRoles.foreach(roleRemoved => dBTransaction.delete(roleRemoved))
    newCaseTeamUserRoles.foreach(roleUpdate => dBTransaction.upsert(roleUpdate))
    removedCaseTeamTenantRoleRoles.foreach(roleRemoval => dBTransaction.delete(roleRemoval))
    newCaseTeamTenantRoleRoles.foreach(roleUpdate => dBTransaction.upsert(roleUpdate))
    removedGroupMappings.foreach(groupMapping => dBTransaction.delete(groupMapping))
    newCaseTeamGroupMappings.foreach(groupMapping => dBTransaction.upsert(groupMapping))
    deletedMembers.foreach(dBTransaction.deleteCaseTeamMember)
  }
}

case class CaseTeamMemberKey(caseInstanceId: String, memberId: String, memberType: MemberType)
