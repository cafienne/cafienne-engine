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

package com.casefabric.cmmn.actorapi.command.team

import com.typesafe.scalalogging.LazyLogging
import com.casefabric.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent
import com.casefabric.cmmn.actorapi.event.team.deprecated.member.{CaseOwnerAdded, CaseOwnerRemoved, TeamRoleCleared, TeamRoleFilled}
import com.casefabric.cmmn.actorapi.event.team.deprecated.user.{TeamMemberAdded, TeamMemberRemoved}
import com.casefabric.cmmn.instance.team.{MemberType, Team}
import com.casefabric.infrastructure.serialization.Fields
import com.casefabric.json._

import java.util
import scala.jdk.CollectionConverters._

case class CaseTeamTenantRole(tenantRoleName: String, override val caseRoles: Set[String] = Set(), override val isOwner: Boolean = false, override val rolesRemoved: Set[String] = Set()) extends CaseTeamMember {
  override val isTenantRole: Boolean = true
  override val memberType: MemberType = MemberType.TenantRole

  override def memberId: String = tenantRoleName

  override def currentMember(team: Team): CaseTeamMember = team.getTenantRole(tenantRoleName)

  override def toValue: Value[_] = {
    val json = memberKeyJson.plus(Fields.isOwner, isOwner, Fields.caseRoles, caseRoles)
    jsonPlusOptionalField(json, Fields.rolesRemoved, rolesRemoved)
  }

  override def memberKeyJson: ValueMap = new ValueMap(Fields.tenantRole, tenantRoleName)

  override def generateChangeEvent(team: Team, newRoles: Set[String]): Unit = team.setTenantRole(this.copy(caseRoles = newRoles))

  def differsFrom(that: CaseTeamTenantRole): Boolean = {
    !(this.tenantRoleName.equals(that.tenantRoleName) &&
      this.isOwner == that.isOwner &&
      this.caseRoles.diff(that.caseRoles).isEmpty && that.caseRoles.diff(this.caseRoles).isEmpty)
  }

  def minus(existingUserInfo: CaseTeamTenantRole): CaseTeamTenantRole = {
    val removedRoles = existingUserInfo.caseRoles.diff(caseRoles)
    this.copy(rolesRemoved = removedRoles)
  }
}

object CaseTeamTenantRole extends LazyLogging {
  def getDeserializer(parent: ValueMap): CaseTeamMemberDeserializer[CaseTeamTenantRole] = {
    // Special deserializer that can migrate rolesRemoved from parent json for older event format (CaseFabric versions 1.1.16 - 1.1.18)
    (json: ValueMap) => {
      if (parent.has(Fields.rolesRemoved)) {
        parent.withArray(Fields.rolesRemoved).getValue.asScala.foreach(json.withArray(Fields.rolesRemoved).add(_))
      }
      CaseTeamTenantRole.deserialize(json)
    }
  }

  def deserialize(json: ValueMap): CaseTeamTenantRole = {
    new CaseTeamTenantRole(
      tenantRoleName = json.readString(Fields.tenantRole),
      caseRoles = json.readStringList(Fields.caseRoles).toSet,
      isOwner = json.readBoolean(Fields.isOwner),
      rolesRemoved = json.readStringList(Fields.rolesRemoved).toSet)
  }

  def handleDeprecatedTenantRoleEvent(tenantRoles: util.Map[String, CaseTeamTenantRole], event: DeprecatedCaseTeamEvent): Unit = {
    def getRole: CaseTeamTenantRole = tenantRoles.get(event.memberId)

    def put(role: CaseTeamTenantRole): Unit = tenantRoles.put(role.memberId, role)

    event match {
      case _: CaseOwnerAdded => put(getRole.copy(isOwner = true))
      case _: CaseOwnerRemoved => put(getRole.copy(isOwner = false))
      case event: TeamRoleCleared =>
        if (event.isMemberItself) tenantRoles.remove(event.memberId)
        else put({
          val role = getRole
          role.copy(caseRoles = role.caseRoles -- Set(event.roleName()))
        })
      case event: TeamRoleFilled =>
        if (event.isMemberItself) put(new CaseTeamTenantRole(tenantRoleName = event.memberId, caseRoles = Set(), isOwner = false))
        else put({
          val role = getRole
          role.copy(caseRoles = role.caseRoles ++ Set(event.roleName()))
        })
      case event: TeamMemberAdded => put(new CaseTeamTenantRole(tenantRoleName = event.memberId, caseRoles = event.getRoles.asScala.toSet, isOwner = false))
      case event: TeamMemberRemoved => tenantRoles.remove(event.memberId)
      case other => logger.warn(s"Unexpected deprecated case team event of type ${other.getClass.getName}")
    }
  }
}
