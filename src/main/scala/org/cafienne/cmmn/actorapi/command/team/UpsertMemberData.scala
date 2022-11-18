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

package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.actormodel.identity.Origin
import org.cafienne.cmmn.definition.team.CaseTeamDefinition
import org.cafienne.cmmn.instance.team.{CaseTeamError, Team}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class UpsertMemberData(val id: String, val isUser: Boolean, val caseRoles: Set[String] = Set(), val ownership: Option[Boolean] = None, val removeRoles: Set[String] = Set()) extends CafienneJson {
  override def toValue: Value[_] = {
    val json = new ValueMap(Fields.identifier, id, Fields.isTenantUser, isUser, Fields.caseRoles, caseRoles, Fields.removeRoles, removeRoles)
    ownership.foreach(isOwner => json.plus(Fields.isOwner, isOwner))
    json
  }

  def validateRolesExist(caseDefinition: CaseTeamDefinition): Unit = {
    val blankRoles = (caseRoles ++ removeRoles).filter(roleName => roleName.isBlank)
    val undefinedRoles = (caseRoles ++ removeRoles).filter(roleName => caseDefinition.getCaseRole(roleName) == null)

    if (blankRoles.nonEmpty || undefinedRoles.nonEmpty) {
      if (undefinedRoles.isEmpty) {
        throw new CaseTeamError("An empty role is not permitted")
      } else {
        throw new CaseTeamError("The following role(s) are not defined in the case: " + undefinedRoles.mkString(","))
      }
    }
  }

  def validateNotLastOwner(team: Team): Unit = {
    ownership.foreach(newOwnership => {
      val owners = team.getOwners.asScala
      if (!newOwnership && owners.size == 1) {
        val owner = owners.head
        if ((owner.isInstanceOf[CaseTeamUser] && isUser && owner.memberId == id) || (owner.isInstanceOf[CaseTeamTenantRole] && !isUser && owner.memberId == id)) {
          throw new CaseTeamError("Cnanot remove last owner")
        }
      }
    })
  }

  def asUser(user: CaseTeamUser): CaseTeamUser = {
    if (user == null) {
      CaseTeamUser.from(userId = id, origin = Origin.Tenant, caseRoles = caseRoles, isOwner = ownership.getOrElse(false))
    } else {
      val newOwnership = ownership.getOrElse(user.isOwner)
      val newRoles = (user.caseRoles ++ this.caseRoles) -- this.removeRoles
      user.copy(newOwnership = newOwnership, newRoles = newRoles)
    }
  }
  def asTenantRole(role: CaseTeamTenantRole): CaseTeamTenantRole = {
    if (role == null) {
      CaseTeamTenantRole(tenantRoleName = id, caseRoles = caseRoles, isOwner = ownership.getOrElse(false))
    } else {
      val newOwnership = ownership.getOrElse(role.isOwner)
      val newRoles = (role.caseRoles ++ this.caseRoles) -- this.removeRoles
      role.copy(isOwner = newOwnership, caseRoles = newRoles)
    }
  }
}

object UpsertMemberData {
  def deserialize(json: ValueMap): UpsertMemberData = {
    val id = json.readString(Fields.identifier)
    val isUser = json.readBoolean(Fields.isTenantUser)
    val caseRoles = json.readStringList(Fields.caseRoles).toSet
    val removeRoles = json.readStringList(Fields.removeRoles).toSet
    val ownership: Option[Boolean] = {
      if (json.has(Fields.isOwner)) {
        Some(json.readBoolean(Fields.isOwner))
      } else {
        None
      }
    }
    new UpsertMemberData(id = id, isUser = isUser, caseRoles = caseRoles, ownership = ownership, removeRoles = removeRoles)
  }
}
