package org.cafienne.cmmn.actorapi.command.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.{CaseOwnerAdded, CaseOwnerRemoved, TeamRoleCleared, TeamRoleFilled}
import org.cafienne.cmmn.actorapi.event.team.deprecated.user.{TeamMemberAdded, TeamMemberRemoved}
import org.cafienne.cmmn.instance.team.{MemberType, Team}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json._

import java.util
import scala.jdk.CollectionConverters.SetHasAsScala

case class CaseTeamTenantRole(tenantRoleName: String, override val caseRoles: Set[String] = Set(), override val isOwner: Boolean = false) extends CaseTeamMember {
  override val isTenantRole: Boolean = true
  override val memberType: MemberType = MemberType.TenantRole

  override def memberId: String = tenantRoleName

  override def currentMember(team: Team): CaseTeamMember = team.getTenantRole(tenantRoleName)

  override def toValue: Value[_] = super.toValue.asMap().plus(Fields.tenantRole, tenantRoleName)

  override def generateChangeEvent(team: Team, newRoles: Set[String]): Unit = team.setTenantRole(this.copy(caseRoles = newRoles))
}

object CaseTeamTenantRole extends LazyLogging {
  def deserialize(json: ValueMap): CaseTeamTenantRole = {
    new CaseTeamTenantRole(
      tenantRoleName = json.readString(Fields.tenantRole),
      caseRoles = json.readStringList(Fields.caseRoles).toSet,
      isOwner = json.readBoolean(Fields.isOwner))
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
