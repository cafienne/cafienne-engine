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

import com.casefabric.actormodel.identity.CaseUserIdentity
import com.casefabric.cmmn.definition.team.CaseTeamDefinition
import com.casefabric.cmmn.instance.team.Team
import com.casefabric.infrastructure.serialization.Fields
import com.casefabric.json.{CaseFabricJson, Value, ValueMap}

import scala.jdk.CollectionConverters._

case class CaseTeam(users: Seq[CaseTeamUser] = Seq(),
                    groups: Seq[CaseTeamGroup] = Seq(),
                    tenantRoles: Seq[CaseTeamTenantRole] = Seq()) extends CaseFabricJson {

  /**
    * Validates whether the case team setup matches the case definition
    *
    * @param definition CaseTeamDefinition to validate against
    */
  def validate(definition: CaseTeamDefinition): Unit = {
    users.foreach(m => m.validateRolesExist(definition))
    groups.foreach(m => m.validateRolesExist(definition))
    tenantRoles.foreach(m => m.validateRolesExist(definition))
  }

  def notHasUser(userId: String): Boolean = !users.exists(_.userId == userId)

  def notHasGroup(groupId: String): Boolean = !groups.exists(_.groupId == groupId)

  def notHasTenantRole(role: String): Boolean = !tenantRoles.exists(_.tenantRoleName == role)

  def owners: Seq[CaseTeamMember] = (users ++ tenantRoles ++ groups).filter(member => member.isOwner)

  def getUsers: java.util.List[CaseTeamUser] = users.asJava

  def getGroups: java.util.List[CaseTeamGroup] = groups.asJava

  def getTenantRoles: java.util.List[CaseTeamTenantRole] = tenantRoles.asJava

  def isEmpty: Boolean = users.isEmpty && tenantRoles.isEmpty && groups.isEmpty

  override def toValue: Value[_] = {
    new ValueMap(Fields.users, users, Fields.groups, groups, Fields.tenantRoles, tenantRoles)
  }
}

object CaseTeam {
  def createSubTeam(team: Team, definition: CaseTeamDefinition): CaseTeam = {
    def hasRole(role: String) = definition.getCaseRole(role) != null

    def getRoles(member: CaseTeamMember): Set[String] = member.caseRoles.filter(hasRole)

    def getMappings(member: CaseTeamGroup): Seq[GroupRoleMapping] = member.mappings.map(m => m.copy(caseRoles = m.caseRoles.filter(hasRole)))

    val subCaseUsers = team.getUsers.asScala.map(user => user.copy(newRoles = getRoles(user))).toSeq
    val subCaseGroups = team.getGroups.asScala.map(group => group.copy(mappings = getMappings(group))).toSeq
    val subCaseTenantRoles = team.getTenantRoles.asScala.map(role => role.copy(caseRoles = getRoles(role))).toSeq
    new CaseTeam(users = subCaseUsers, groups = subCaseGroups, tenantRoles = subCaseTenantRoles)
  }

  def create(members: java.util.Collection[CaseTeamUser]) = new CaseTeam(users = members.asScala.toSeq)

  def create(user: CaseUserIdentity) = new CaseTeam(users = Seq(CaseTeamUser.from(userId = user.id, origin = user.origin, isOwner = true)))

  def deserialize(json: ValueMap): CaseTeam = {
    val users = json.readObjects(Fields.users, CaseTeamUser.deserialize).asScala.toSeq
    val groups = json.readObjects(Fields.groups, CaseTeamGroup.deserialize).asScala.toSeq
    val tenantRoles = json.readObjects(Fields.tenantRoles, CaseTeamTenantRole.deserialize).asScala.toSeq
    new CaseTeam(groups = groups, users = users, tenantRoles = tenantRoles)
  }
}
