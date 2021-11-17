package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.actormodel.identity.CaseUserIdentity
import org.cafienne.cmmn.definition.team.CaseTeamDefinition
import org.cafienne.cmmn.instance.team.Team
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import scala.jdk.CollectionConverters._

case class CaseTeam(users: Seq[CaseTeamUser] = Seq(),
                    tenantRoles: Seq[CaseTeamTenantRole] = Seq()) extends CafienneJson {

  /**
    * Validates whether the case team setup matches the case definition
    *
    * @param definition CaseTeamDefinition to validate against
    */
  def validate(definition: CaseTeamDefinition): Unit = {
    users.foreach(m => m.validateRolesExist(definition))
    tenantRoles.foreach(m => m.validateRolesExist(definition))
  }

  def notHasUser(userId: String): Boolean = !users.exists(_.userId == userId)
  def notHasTenantRole(role: String): Boolean = !tenantRoles.exists(_.tenantRoleName == role)

  def owners: Seq[CaseTeamMember] = (users ++ tenantRoles).filter(member => member.isOwner)

  def getUsers: java.util.List[CaseTeamUser] = users.asJava

  def getTenantRoles: java.util.List[CaseTeamTenantRole] = tenantRoles.asJava

  def isEmpty: Boolean = users.isEmpty && tenantRoles.isEmpty

  override def toValue: Value[_] = {
    new ValueMap(Fields.users, users, Fields.tenantRoles, tenantRoles)
  }
}

object CaseTeam {
  def createSubTeam(team: Team, definition: CaseTeamDefinition): CaseTeam = {
    def getRoles(member: CaseTeamMember): Set[String] = member.caseRoles.filter(definition.getCaseRole(_) != null)

    val subCaseUsers = team.getUsers.asScala.map(user => user.copy(newRoles = getRoles(user))).toSeq
    val subCaseTenantRoles = team.getTenantRoles.asScala.map(role => role.copy(caseRoles = getRoles(role))).toSeq
    new CaseTeam(users = subCaseUsers, tenantRoles = subCaseTenantRoles)
  }

  def create(members: java.util.Collection[CaseTeamUser]) = new CaseTeam(users = members.asScala.toSeq)

  def create(user: CaseUserIdentity) = new CaseTeam(users = Seq(CaseTeamUser.from(userId = user.id, origin = user.origin, isOwner = true)))

  def deserialize(json: ValueMap): CaseTeam = {
    val users = json.readObjects(Fields.users, CaseTeamUser.deserialize).asScala.toSeq
    val tenantRoles = json.readObjects(Fields.tenantRoles, CaseTeamTenantRole.deserialize).asScala.toSeq
    new CaseTeam(users = users, tenantRoles = tenantRoles)
  }
}