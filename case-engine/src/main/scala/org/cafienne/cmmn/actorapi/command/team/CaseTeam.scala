package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.actormodel.identity.CaseUserIdentity
import org.cafienne.cmmn.definition.team.CaseTeamDefinition
import org.cafienne.cmmn.instance.team.Team
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import scala.jdk.CollectionConverters._

case class CaseTeam(users: Seq[CaseTeamUser] = Seq(),
                    groups: Seq[CaseTeamGroup] = Seq(),
                    tenantRoles: Seq[CaseTeamTenantRole] = Seq()) extends CafienneJson {

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
