package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.cmmn.definition.team.CaseTeamDefinition
import org.cafienne.cmmn.instance.team.{CaseTeamError, MemberType, Team}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json._

import java.util
import scala.jdk.CollectionConverters._

trait CaseTeamMember extends CafienneJson {
  val caseRoles: Set[String] = Set()
  val isOwner: Boolean = false

  val isUser = false
  val isTenantRole = false
  val isGroup = false

  val memberType: MemberType
  def memberId: String

  lazy val description: String = s"$memberType - $memberId"

  def currentMember(team: Team): CaseTeamMember

  def validateRolesExist(definition: CaseTeamDefinition): Unit = {
    val allRolesUnderProcessing = caseRoles
    val blankRoles = allRolesUnderProcessing.filter(roleName => roleName.isBlank)
    val undefinedRoles = allRolesUnderProcessing.filter(roleName => definition.getCaseRole(roleName) == null)

    if (blankRoles.nonEmpty || undefinedRoles.nonEmpty) {
      if (undefinedRoles.isEmpty) {
        throw new CaseTeamError("An empty role is not permitted")
      } else {
        throw new CaseTeamError("The following role(s) are not defined in the case: " + undefinedRoles.mkString(","))
      }
    }
  }

  def generateChangeEvent(team: Team, newRoles: Set[String]): Unit

  def migrateRoles(team: Team, changedRoleNames: util.Map[String, String], droppedRoles: util.Set[String]): Unit = {
    val newRoles = this.caseRoles.filterNot(droppedRoles.contains(_)).map(role =>changedRoleNames.getOrDefault(role, role))
    generateChangeEvent(team, newRoles)
  }

  def getCaseRoles: util.Set[String] = {
    caseRoles.asJava
  }

  override def toValue: Value[_] = {
    val json = new ValueMap(Fields.isOwner, isOwner, Fields.caseRoles, caseRoles)
    json
  }
}
